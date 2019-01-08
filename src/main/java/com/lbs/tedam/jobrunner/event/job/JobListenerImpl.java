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

package com.lbs.tedam.jobrunner.event.job;

import java.util.List;

import com.lbs.tedam.data.service.JobGroupService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.manager.JobRunnerScheduler;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobGroup;
import com.lbs.tedam.notification.NotifierFactory;
import com.lbs.tedam.util.EnumsV2.JobStatus;
import com.lbs.tedam.util.EnumsV2.NotificationType;

public class JobListenerImpl implements JobListener {

	private JobGroupService jobGroupService;
	private JobRunnerScheduler jobRunnerScheduler;

	public JobListenerImpl(JobGroupService jobGroupService, JobRunnerScheduler jobRunnerScheduler) {
		this.jobGroupService = jobGroupService;
		this.jobRunnerScheduler = jobRunnerScheduler;
	}

	@Override
	public void onJobComplete(JobEvent event) throws LocalizedException {
		Job job = event.getJob();
		sendNotification(job);
		startNextGroupJob(job);
	}

	private void sendNotification(Job job) {
		if (job.getNotificationGroup() != null && job.getNotificationGroup().getType() != null) {
		NotificationType notificationType = job.getNotificationGroup().getType();
			NotifierFactory.getNotifier(notificationType).sendNotification(job);
		}
	}

	private void startNextGroupJob(Job job) throws LocalizedException {
		Integer jobGroupId = job.getJobGroupId();
		if (jobGroupId != null && jobGroupId.equals(Integer.valueOf(0)) == false) {
			JobGroup jobGroup = jobGroupService.getById(jobGroupId);
			List<Job> jobs = jobGroup.getJobs();
			if (jobs != null && jobs.size() > 0) {
				checkForNextGroupJob(job, jobGroupId, jobs);
				checkForJobGroupComplete(job, jobGroup);
			}
		}
	}

	private void checkForNextGroupJob(Job job, Integer jobGroupId, List<Job> jobs) throws LocalizedException {
		boolean jobFound = false;
		for (Job jobVar : jobs) {
			if (jobFound) { // we found executed job one step before. So this job must be executed because
							// of group execution.
				jobVar.setJobGroupId(jobGroupId);
				jobRunnerScheduler.scheduleJob(jobVar);
				break;
			}
			else if (job.equals(jobVar)) {
				jobFound = true; // we found executed job in this step. so in next step we will execute next
									// job.
			}
		}
	}

	private void checkForJobGroupComplete(Job job, JobGroup jobGroup) throws LocalizedException {
		List<Job> jobs = jobGroup.getJobs();
		Job lastJob = jobs.get(jobs.size() - 1);
		if (job.equals(lastJob)) {
			jobGroup.setStatus(JobStatus.COMPLETED);
			jobGroupService.save(jobGroup);
		}
	}

}
