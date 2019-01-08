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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.lbs.tedam.data.service.DraftCommandService;
import com.lbs.tedam.data.service.JobDetailService;
import com.lbs.tedam.data.service.JobParameterService;
import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.data.service.TestCaseService;
import com.lbs.tedam.data.service.TestSetService;
import com.lbs.tedam.exception.JobCommandBuildException;
import com.lbs.tedam.exception.TedamDatabaseException;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.service.JobRunnerEngineService;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.DraftCommand;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobCommand;
import com.lbs.tedam.model.JobDetail;
import com.lbs.tedam.model.JobParameter;
import com.lbs.tedam.model.JobParameterValue;
import com.lbs.tedam.model.TestCase;
import com.lbs.tedam.model.TestSet;
import com.lbs.tedam.model.TestStep;
import com.lbs.tedam.util.EnumsV2.CommandStatus;
import com.lbs.tedam.util.EnumsV2.JobParameterType;
import com.lbs.tedam.util.EnumsV2.StaticJobParameter;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamStringUtils;

public class JobRunnerEngineServiceImpl implements JobRunnerEngineService, HasLogger {

	private JobService jobService;
	private JobDetailService jobDetailService;
	private TestSetService testSetService;
	private TestCaseService testCaseService;
	private DraftCommandService draftCommandService;
	private JobParameterService jobParameterService;

	@Autowired
	public JobRunnerEngineServiceImpl(JobService jobService, JobDetailService jobDetailService, TestSetService testSetService, TestCaseService testCaseService,
			DraftCommandService draftCommandService, JobParameterService jobParameterService) {
		this.jobService = jobService;
		this.jobDetailService = jobDetailService;
		this.testSetService = testSetService;
		this.testCaseService = testCaseService;
		this.draftCommandService = draftCommandService;
		this.jobParameterService = jobParameterService;
	}

	/*
	 * (non-Javadoc)
	 * @see com.tedamface.view.jobrunner.IJobRunnerEngineService#setJobDetailAttributes(com.lbs.tedam.model.JobDetail)
	 */
	@Override
	public JobDetail setJobDetailAttributes(JobDetail jobDetail) throws JobCommandBuildException, LocalizedException {
		getLogger().info("Method setJobDetailAttributes starts.");
		// The job commands for job detail are created and the list is filled.
		List<JobCommand> jobCommandList = buildJobCommand(jobDetail);
		// In each job queue, the jobCommands attached to the jobDetail must be re-inserted into the database so that the jobCommands for each jobDetail will be re-created.
		jobDetail.setJobCommands(jobCommandList);
		getLogger().info("jobDetailId : " + jobDetail.getId() + " for " + jobCommandList.size() + " until the command was created.");
		// the created list and the job detail are sent for saving.
		JobDetail saveJobDetailAndClient = saveJobDetailAndClient(jobDetail);
		saveJobDetailAndClient.setJobGroupId(jobDetail.getJobGroupId());
		getLogger().info("Method setJobDetailAttributes is over.");
		return saveJobDetailAndClient;
	}

	/**
	 * this method saveJobDetailAndClient jobDetail saves. Update client status and save.<br>
	 * @author Tarik.Mikyas
	 * @param jobDetail
	 *            <br>
	 * @throws LocalizedException 
	 */
	private JobDetail saveJobDetailAndClient(JobDetail jobDetail) throws LocalizedException {
		JobDetail saveJobDetail = null;
		try {
			Client client = jobDetail.getClient();
			getLogger().info(" clientId : " + client.getId() + " clientName : " + client.getName());
			saveJobDetail = jobDetailService.save(jobDetail);
			getLogger().info(" jobDetailId : " + jobDetail.getId() + " testSetId : " + jobDetail.getTestSet().getId() + " was recorded.");
		} catch (NullPointerException e) {
			getLogger().error("jobDetailId : " + jobDetail.getId() + " jobDetail.getClient() : " + jobDetail.getClient() + " " + e);
		}
		if (saveJobDetail != null)
			return saveJobDetail;
		return jobDetail;
	}

	/**
	 * this method buildJobCommand <br>
	 * 
	 * job commands are created according to given jobDetail.
	 * 
	 * @author Canberk.Erkmen
	 * @param jobDetail
	 * @return <br>
	 * @throws JobCommandBuildException
	 * @throws LocalizedException 
	 */
	public List<JobCommand> buildJobCommand(JobDetail jobDetail) throws JobCommandBuildException, LocalizedException {
		getLogger().info("buildJobCommand (jobDetailId : " + jobDetail.getId() + ") started.");
		// the testCaseList of the relevant testSet is pulled.
		List<TestCase> testCaseList = testSetService.getById(jobDetail.getTestSet().getId()).getTestCasesOrdered();
		List<JobCommand> jobCommandList = new ArrayList<>();
		getLogger().info("All jobCommands are being created in buildJobCommand.");
		for (TestCase testCase : testCaseList) {
			List<JobCommand> resultList = createJobCommandListByDraftCommands(testCase, jobDetail);
			jobCommandList.addAll(resultList);
		}
		getLogger().info("The buildJobCommand jobCommand was created. (jobDetailId : " + jobDetail.getId() + ") it's over");
		return jobCommandList;
	}

	/**
	 * this method createJobCommandListByDraftCommands is creates a command for a given testCase up to a defined draftCommand.<br>
	 * @author Tarik.Mikyas
	 * @param testCase
	 * @param draftCommandList
	 * @param jobDetail
	 * @return
	 * @throws JobCommandBuildException
	 *             <br>
	 * @throws LocalizedException 
	 */
	private List<JobCommand> createJobCommandListByDraftCommands(TestCase testCase, JobDetail jobDetail) throws JobCommandBuildException, LocalizedException {
		Job job = jobService.getById(jobDetail.getJobId());
		List<DraftCommand> draftCommandList = draftCommandService.getDraftCommandListByProject(job.getProject());
		getLogger().info("createJobCommandListByDraftCommands jobDetailId : " + jobDetail.getId() + " starting.");
		List<JobCommand> jobCommandList = new ArrayList<>();
		List<JobParameterValue> jobParameterValueList = job.getJobEnvironment().getJobParameterValues();
		List<JobParameter> jobParameterList = jobParameterService.getJobParameterListByProject(job.getProject());
		getLogger().info("createJobCommandListByDraftCommands jobParameterValueList created. : size :" + jobParameterValueList.size());
		Map<String, String> staticJobParameterValueMap = getStaticJobParameterValueMap(jobDetail.getTestSet().getId(), testCase.getId(), jobDetail.getClient().getName());
		getLogger().info("createJobCommandListByDraftCommands staticJobParameterValueMap created. : size :" + staticJobParameterValueMap.size());
		for (DraftCommand draftCommand : draftCommandList) {
			JobCommand jobCommand = new JobCommand();
			jobCommand.setDraftCommand(draftCommand);
			jobCommand.setCommandStatus(CommandStatus.NOT_STARTED);
			jobCommand.setTestCase(testCase);

			String draftCommandWindowsValue = changeDraftCommandWithParameter(staticJobParameterValueMap, jobParameterValueList, draftCommand.getWindowsValue(), jobParameterList);
			String draftCommandUnixValue = changeDraftCommandWithParameter(staticJobParameterValueMap, jobParameterValueList, draftCommand.getUnixValue(), jobParameterList);

			jobCommand.setWindowsCommand(draftCommandWindowsValue);
			jobCommand.setUnixCommand(draftCommandUnixValue);

			jobCommandList.add(jobCommand);
		}
		getLogger().info("createJobCommandListByDraftCommands jobDetailId : " + jobDetail.getId() + " for the jobCommandList was created. size : " + jobCommandList.size());
		return jobCommandList;
	}

	/**
	 * this method getStaticJobParameterValueMap is Create and dispatch the values of the job parameters defined in the system, the map in which the static variables are kept.<br>
	 * @author Canberk.Erkmen
	 * @author Tarik.Mikyas
	 * @param testSetId
	 * @param testCaseId
	 * @return
	 * @throws JobCommandBuildException
	 *             <br>
	 */
	private Map<String, String> getStaticJobParameterValueMap(int testSetId, int testCaseId, String clientToken) throws JobCommandBuildException {
		getLogger().info("initializeStaticJobParameterValueMap testSetId :" + testSetId + " testCaseId :" + testCaseId + " starts for.");
		Map<String, String> staticJobParameterValueMap = new HashMap<>();
		staticJobParameterValueMap.put(StaticJobParameter.TEST_CASE_ID.getValue(), String.valueOf(testCaseId));
		staticJobParameterValueMap.put(StaticJobParameter.TEST_SET_ID.getValue(), String.valueOf(testSetId));
		staticJobParameterValueMap.put(StaticJobParameter.TEST_STEPS.getValue(), findTestStepParameters(testCaseId));
		staticJobParameterValueMap.put(StaticJobParameter.CLIENT_HOST_NAME.getValue(), clientToken);
		getLogger().info("initializeStaticJobParameterValueMap testSetId :" + testSetId + " testCaseId :" + testCaseId + " It's over.");
		return staticJobParameterValueMap;
	}

	/**
	 * this method changeDraftCommandWithParameter is It takes the draft command as a parameter and sets the static and user parameters to create the unix job command. <br>
	 * @author Canberk.Erkmen
	 * @param staticJobParameterValueMap
	 * @param jobParameterValueList
	 * @param jobParameterList
	 * @param unixValue
	 * @return <br>
	 * @throws JobCommandBuildException
	 */
	private String changeDraftCommandWithParameter(Map<String, String> staticJobParameterValueMap, List<JobParameterValue> jobParameterValueList, String draftCommandValue,
			List<JobParameter> jobParameterList) throws JobCommandBuildException {
		String editedDraftCommandValue = draftCommandValue;
		try {

			for (JobParameterValue jobParameterValue : jobParameterValueList) {
				// If the value of jobParameter belongs to value, it is replaced with the value of value.
				JobParameter jobParameter = findJobParameterById(jobParameterList, jobParameterValue.getJobParameterId());
				if (editedDraftCommandValue.contains(JobParameterType.CONSTANT.getSign() + jobParameter.getName())) {
					editedDraftCommandValue = editedDraftCommandValue.replace(JobParameterType.CONSTANT.getSign() + jobParameter.getName(), jobParameterValue.getValue());
				}
			}

			for (StaticJobParameter staticJobParameter : EnumSet.allOf(StaticJobParameter.class)) {
				if (editedDraftCommandValue.contains(JobParameterType.STATIC.getSign() + staticJobParameter.getValue())) {
					editedDraftCommandValue = editedDraftCommandValue.replace(JobParameterType.STATIC.getSign() + staticJobParameter.getValue(),
							staticJobParameterValueMap.get(staticJobParameter.getValue()));
				}
			}
		} catch (Exception e) {
			throw new JobCommandBuildException("ERROR!. There was an error setting the constant and static variables in the DraftCommand. e : " + e);
		}
		return editedDraftCommandValue;
	}

	private JobParameter findJobParameterById(List<JobParameter> jobParameterList, Integer jobParameterId) throws JobCommandBuildException {
		for (JobParameter jobParameter : jobParameterList) {
			if (jobParameter.getId().equals(jobParameterId)) {
				return jobParameter;
			}
		}
		throw new JobCommandBuildException("JobParameter not found by id: " + jobParameterId.intValue());
	}

	/**
	 * this method findTestStepParameters is TestCase will bring up the TestStep parameters.<br>
	 * @author Canberk.Erkmen
	 * @param testCaseId
	 * @return <br>
	 * @throws JobCommandBuildException
	 */
	private String findTestStepParameters(int testCaseId) throws JobCommandBuildException {
		getLogger().info("findTestStepParameters testCaseId :" + testCaseId + " starts for.");
		// TestSteps are populated according to testCaseId and filled in testStepList.
		List<TestStep> testStepList = new ArrayList<>();
		try {
			testStepList = testCaseService.getById(Integer.valueOf(testCaseId)).getTestSteps();
			if (testStepList.isEmpty()) {
				throw new TedamDatabaseException("testStepList is empty ! ");
			}
		} catch (NumberFormatException | TedamDatabaseException e) {
			getLogger().error("" + e);
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
		}
		// There are parameters for testStepList.
		String parameters = TedamStringUtils.findTestParamaters(testStepList);
		getLogger().info("findTestStepParameters testCaseId :" + testCaseId + " is over for. parameters.length : " + parameters.length());
		return parameters;
	}

	/**
	 * this method isJobJobDetailsEmpty is One of the testDetails in jobDetails of the job is checked to see if there is a TestSet in SPIRA.<br>
	 * @author Canberk.Erkmen
	 * @param job
	 * @return <br>
	 * @throws LocalizedException 
	 */
	@Override
	public boolean isJobJobDetailEmpty(JobDetail jobDetail) throws LocalizedException {
		TestSet testSet = jobDetail.getTestSet();
		boolean isTestSetExist = testSet != null && testSet.getId() > Integer.valueOf(0);
		boolean isTestCaseExistInTestSet = isTestCaseExistInTestSet(jobDetail.getTestSet().getId());
		if (!isTestSetExist || !isTestCaseExistInTestSet) {
			getLogger().info("jobDetailId : " + jobDetail.getId() + " testSetId : " + jobDetail.getTestSet().getId() + "isTestSetExist : " + isTestSetExist
					+ " isTestCaseExistInTestSet : " + isTestCaseExistInTestSet);
			return true;
		}
		return false;
	}

	/**
	 * this method cleanJobDetails is Checks whether the testSet in the jobDetails of the job is in SPIRA, if the testSetList is empty, it is deleted from the jobDetail job.<br>
	 * then the job is submitted for update to the updateOrDeleteJob method and the return value is returned.
	 * @author Canberk.Erkmen
	 * @param job
	 * @return <br>
	 * @throws LocalizedException 
	 */
	// TODO
	@Override
	public void removeJobDetailsAndUpdateJob(JobDetail jobDetail) throws LocalizedException {
		Job job = jobService.getById(jobDetail.getJobId());
		getLogger().info("jobDetailId : " + jobDetail.getId() + " jobId : " + job.getId() + " will be deleted");
		job.getJobDetails().remove(jobDetail);
		updateOrDeleteJob(job, jobDetail.getTestSet().getId());
	}

	/**
	 * this method isTestCaseListInTestSetListEmpty <br>
	 * @author Canberk.Erkmen
	 * @param testSetId
	 * @return <br>
	 * @throws LocalizedException 
	 */
	private boolean isTestCaseExistInTestSet(int testSetId) throws LocalizedException {
		TestSet testSet = testSetService.getById(testSetId);
		boolean isTestCaseExist = testSet != null && testSet.getTestCases() != null && testSet.getTestCases().size() > 0;
		return isTestCaseExist;
	}

	/**
	 * this method updateOrDeleteJob the given jobin is updated if the jobDetails are present. If the jobDetails are not available, the job is deleted.<br>
	 * if delete is done, isDeleted = true is set.
	 * @author Canberk.Erkmen
	 * @param job
	 * @return <br>
	 * @throws LocalizedException 
	 */
	private void updateOrDeleteJob(Job job, int testSetId) throws LocalizedException {
		if (job.getJobDetails().isEmpty()) {
			jobService.deleteById(job.getId());
			getLogger().info("jobId" + job.getId() + " deleted.");
		} else {
			jobService.save(job);
		}
	}

}
