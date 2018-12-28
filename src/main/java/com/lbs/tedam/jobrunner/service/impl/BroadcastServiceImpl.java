/*
* Copyright 2014-2019 Logo Business Solutions
* (a.k.a. LOGO YAZILIM SAN. VE TIC. A.S)
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

package com.lbs.tedam.jobrunner.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.lbs.tedam.data.service.JobCommandService;
import com.lbs.tedam.data.service.JobDetailService;
import com.lbs.tedam.data.service.JobRunnerDetailCommandService;
import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.exception.VersionParameterValueException;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.event.job.JobEvent;
import com.lbs.tedam.jobrunner.event.job.JobListener;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.manager.JobRunnerManager;
import com.lbs.tedam.jobrunner.manager.JobRunnerScheduler;
import com.lbs.tedam.jobrunner.service.BroadcastService;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobCommand;
import com.lbs.tedam.model.JobDetail;
import com.lbs.tedam.model.JobRunnerDetailCommand;
import com.lbs.tedam.util.EnumsV2.ClientStatus;
import com.lbs.tedam.util.EnumsV2.CommandStatus;
import com.lbs.tedam.util.EnumsV2.JobStatus;
import com.lbs.tedam.util.EnumsV2.RunOrder;
import com.lbs.tedam.util.HasLogger;

public class BroadcastServiceImpl implements BroadcastService, HasLogger {

	// private JobReportService jobReportService = new JobXMLReportService();
	private final JobService jobService;
	private final JobCommandService jobCommandService;
	private final JobDetailService jobDetailService;
	private final ClientMapService clientMapService;
	private final JobRunnerDetailCommandService jobRunnerDetailCommandService;
	private final JobRunnerManager jobRunnerManager;
	private final List<JobListener> jobListenerList = new ArrayList<JobListener>();

	@Autowired
	private JobRunnerScheduler jobRunnerScheduler;

	public BroadcastServiceImpl(JobRunnerManager jobRunnerManager, ClientMapService clientMapService,
			JobService jobService, JobCommandService jobCommandService, JobDetailService jobDetailService,
			JobRunnerDetailCommandService jobRunnerDetailCommandService) {
		this.jobRunnerManager = jobRunnerManager;
		this.jobService = jobService;
		this.jobCommandService = jobCommandService;
		this.jobDetailService = jobDetailService;
		this.clientMapService = clientMapService;
		this.jobRunnerDetailCommandService = jobRunnerDetailCommandService;
	}

	public void addJobListener(JobListener jobListener) {
		jobListenerList.add(jobListener);
	}

	/**
	 * this method startJobCommandOperations <br>
	 * 
	 * @author Canberk.Erkmen
	 * @param jobCommandId
	 * @param commandStatusCode
	 * @param result
	 * @param description       <br>
	 * @throws VersionParameterValueException
	 * @throws LocalizedException
	 */
	@Override
	public void startJobCommandOperations(JobRunnerDetailCommand jobRunnerDetailCommand)
			throws VersionParameterValueException, LocalizedException {
		JobCommand jobCommand = jobRunnerDetailCommandService
				.getJobCommandByJobRunnerDetailCommand(jobRunnerDetailCommand);
		jobCommandService.updateJobCommand(jobCommand);
		JobDetail jobDetail = jobDetailService.getById(jobCommand.getJobDetailId());
		Job job = jobService.getById(jobDetail.getJobId());
		// TODO: will be checked later
		// buildJobReport(jobCommand, jobDetail);
		if (RunOrder.RUN_SCRIPT.equals(jobCommand.getDraftCommand().getRunOrder())
				&& CommandStatus.getCompletedCommandStatus().contains(jobRunnerDetailCommand.getCommandStatus())) {
			startTestRunOperations(jobRunnerDetailCommand, jobCommand, jobDetail, job);
		}
		if (CommandStatus.NOT_STARTED == jobDetail.getStatus()) {
			startJobDetailInProgressOperations(jobDetail);
			if (JobStatus.STARTED != job.getStatus()) {
				jobService.updateJobStatusAndExecutedDateByJobId(jobDetail.getJobId(), JobStatus.STARTED,
						LocalDateTime.now(), null);
			}
		} else if (isJobDetailJobCommandListFinished(jobDetail)) {
			startJobDetailCompletedOperations(jobDetail);
			if (isJobCompleted(job, jobDetail)) {
				startJobCompletedOperations(jobDetail, job);
				callOnJobComplete(job);
			}
		}
		if (isJobStopped(job)) {
			getLogger().info("The job with id: " + jobDetail.getJobId() + " change to STOPPED");
			jobService.updateJobStatusAndExecutedDateByJobId(jobDetail.getJobId(), JobStatus.STOPPED,
					job.getLastExecutedStartDate(), null);
			return;
		}

	}

	private void startTestRunOperations(JobRunnerDetailCommand jobRunnerDetailCommand, JobCommand jobCommand,
			JobDetail jobDetail, Job job) throws VersionParameterValueException, LocalizedException {
		jobRunnerDetailCommandService.createTestRunForTestCaseAndTestStep(jobRunnerDetailCommand, jobCommand, jobDetail,
				job);
	}

	private void startJobCompletedOperations(JobDetail jobDetail, Job job) throws LocalizedException {
		getLogger().info("The job with id: " + jobDetail.getJobId() + " finished");
		jobService.updateJobStatusAndExecutedDateByJobId(jobDetail.getJobId(), JobStatus.COMPLETED,
				job.getLastExecutedStartDate(), LocalDateTime.now());
		if (job.getPlannedDate() != null && job.isRunEveryDay()) {
			addOneDayMore(job);
		} else {
			jobService.resetJobPlannedDate(job.getId());
		}
		// String jobReportFilePath =
		// propertyService.getPropertyByNameAndParameter(Constants.PROPERTY_CONFIG,
		// Constants.PROPERTY_TEMP_FILE_PATH).getValue();
		// jobReportService.createJobReportFile(jobDetail.getJobId(),
		// jobReportFilePath);
		// jobReportService.removeJobReportMap(jobDetail.getJobId());
		jobRunnerManager.removeJobFromRunningJobs(job);
	}

	private void startJobDetailInProgressOperations(JobDetail jobDetail) throws LocalizedException {
		jobDetail.setStatus(CommandStatus.IN_PROGRESS);
		jobDetail.getTestSet().setTestSetStatus(CommandStatus.IN_PROGRESS);
		jobDetailService.save(jobDetail);
	}

	private void startJobDetailCompletedOperations(JobDetail jobDetail) throws LocalizedException {
		getLogger().info("The jobdetail with testSetId: " + jobDetail.getTestSet().getId() + " and with jobDetailId:  "
				+ jobDetail.getId() + " finished");
		Client client = jobDetail.getClient();
		jobDetail.setStatus(CommandStatus.COMPLETED);
		jobDetail.getTestSet().setTestSetStatus(CommandStatus.COMPLETED);
		jobDetail.getTestSet().setExecutionDateTime(LocalDateTime.now());
		jobDetail.getJobCommands().clear();
		getLogger()
				.info("clientName : " + client.getName() + " clientId : " + client.getId() + " will be set to free.");
		clientMapService.updateClientMap(client, ClientStatus.FREE);
		jobDetail.setClient(null);
		jobDetailService.save(jobDetail);

	}

	// private void buildJobReport(JobCommand jobCommand, JobDetail jobDetail) {
	// JobReport jobReport =
	// jobReportService.getJobReportByParams(jobCommand.getTestCase(), jobDetail);
	// JobReportResult jobReportResult =
	// jobReportService.getJobReportResultByParams(jobCommand);
	// jobReportService.addJobReportMap(jobReport, jobReportResult);
	// }

	public boolean isJobCompleted(Job job, JobDetail lastJobDetail) {
		for (JobDetail jobDetail : job.getJobDetails()) {
			// If this jobDetail is the last completed one, it is skipped.
			if (jobDetail.equals(lastJobDetail) && CommandStatus.COMPLETED.equals(lastJobDetail.getStatus())) {
				continue;
			}
			// If any job detail status is not completed, false is returned because
			// jobDetailList is not complete.
			if (!CommandStatus.COMPLETED.equals(jobDetail.getStatus())) {
				return false;
			}
		}
		return true;
	}

	public boolean isJobInProgress(Job job) {
		for (JobDetail jobDetail : job.getJobDetails()) {
			// If any job detail status is not completed, false is returned if jobDetailList
			// is not complete.
			if (CommandStatus.IN_PROGRESS.equals(jobDetail.getStatus())) {
				return true;
			}
		}
		return false;
	}

	public boolean isJobStopped(Job job) {
		if (JobStatus.WAITING_STOP.equals(job.getStatus()) && !isJobInProgress(job)) {
			return true;
		}
		return false;
	}

	public boolean isJobDetailJobCommandListFinished(JobDetail jobDetail) {
		for (JobCommand jobCommand : jobDetail.getJobCommands()) {
			if (!CommandStatus.COMPLETED.equals(jobCommand.getCommandStatus())
					&& !CommandStatus.BLOCKED.equals(jobCommand.getCommandStatus())) {
				return false;
			}
		}
		return true;
	}

	private void addOneDayMore(Job job) throws LocalizedException {
		job.setStatus(JobStatus.PLANNED);
		job.setPlannedDate(job.getPlannedDate().plusDays(1));
		jobRunnerScheduler.scheduleJob(job);
		jobService.save(job);
	}

	private void callOnJobComplete(Job job) {
		for (JobListener listener : jobListenerList) {
			JobEvent event = new JobEvent(job);
			listener.onJobComplete(event);
		}
	}
}
