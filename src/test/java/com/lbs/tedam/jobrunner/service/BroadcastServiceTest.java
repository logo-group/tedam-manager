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

package com.lbs.tedam.jobrunner.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.lbs.tedam.data.service.JobCommandService;
import com.lbs.tedam.data.service.JobDetailService;
import com.lbs.tedam.data.service.JobRunnerDetailCommandService;
import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.exception.VersionParameterValueException;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.manager.JobRunnerManager;
import com.lbs.tedam.jobrunner.service.impl.BroadcastServiceImpl;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.DraftCommand;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobCommand;
import com.lbs.tedam.model.JobDetail;
import com.lbs.tedam.model.JobRunnerDetailCommand;
import com.lbs.tedam.model.TestSet;
import com.lbs.tedam.util.EnumsV2.CommandStatus;
import com.lbs.tedam.util.EnumsV2.JobStatus;

public class BroadcastServiceTest {

	private JobService jobService;
	private JobCommandService jobCommandService;
	private JobDetailService jobDetailService;
	private ClientMapService clientMapService;
	private JobRunnerDetailCommandService jobRunnerDetailCommandService;
	private JobRunnerManager jobRunnerManager;
	private BroadcastServiceImpl broadCastService;

	@Before
	public void beforeTest() {
		jobService = mock(JobService.class);
		jobCommandService = mock(JobCommandService.class);
		jobDetailService = mock(JobDetailService.class);
		clientMapService = mock(ClientMapService.class);
		jobRunnerDetailCommandService = mock(JobRunnerDetailCommandService.class);
		jobRunnerManager = mock(JobRunnerManager.class);
		broadCastService = new BroadcastServiceImpl(jobRunnerManager, clientMapService, jobService, jobCommandService,
				jobDetailService, jobRunnerDetailCommandService);
	}

	@Test
	public void isJobDetailJobCommandListFinishedTestWithEmptyCommandList() {
		JobDetail jobDetail = new JobDetail();
		assertTrue(broadCastService.isJobDetailJobCommandListFinished(jobDetail));
	}

	@Test
	public void isJobDetailJobCommandListFinishedTestWithBlockStatus() {
		JobDetail jobDetail = new JobDetail();
		JobCommand jobCommand = new JobCommand();
		jobCommand.setCommandStatus(CommandStatus.BLOCKED);
		jobDetail.getJobCommands().add(jobCommand);
		assertTrue(broadCastService.isJobDetailJobCommandListFinished(jobDetail));
	}

	@Test
	public void isJobDetailJobCommandListFinishedTestWithCompletedStatus() {
		JobDetail jobDetail = new JobDetail();
		JobCommand jobCommand = new JobCommand();
		jobCommand.setCommandStatus(CommandStatus.COMPLETED);
		jobDetail.getJobCommands().add(jobCommand);
		assertTrue(broadCastService.isJobDetailJobCommandListFinished(jobDetail));
	}

	@Test
	public void isJobDetailJobCommandListFinishedTest() {
		JobDetail jobDetail = new JobDetail();
		JobCommand jobCommand = new JobCommand();
		jobCommand.setCommandStatus(CommandStatus.IN_PROGRESS);
		jobDetail.getJobCommands().add(jobCommand);
		assertFalse(broadCastService.isJobDetailJobCommandListFinished(jobDetail));
	}

	@Test
	public void isJobStoppedTest() {
		Job job = new Job();
		job.setStatus(JobStatus.STARTED);
		assertFalse(broadCastService.isJobStopped(job));
	}

	@Test
	public void isJobStoppedTestWithWaitingStop() {
		Job job = new Job();
		job.setStatus(JobStatus.WAITING_STOP);
		assertTrue(broadCastService.isJobStopped(job));
	}

	@Test
	public void isJobStoppedTestWithInProgressDetail() {
		Job job = new Job();
		JobDetail jobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.IN_PROGRESS);
		job.getJobDetails().add(jobDetail);
		job.setStatus(JobStatus.NOT_STARTED);
		assertFalse(broadCastService.isJobStopped(job));
	}

	@Test
	public void isJobStoppedTestWithBlockedStatus() {
		Job job = new Job();
		JobDetail jobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.BLOCKED);
		job.getJobDetails().add(jobDetail);
		job.setStatus(JobStatus.WAITING_STOP);
		assertTrue(broadCastService.isJobStopped(job));
	}

	@Test
	public void isJobStoppedTestWithCompletedStatus() {
		Job job = new Job();
		JobDetail jobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.BLOCKED);
		job.getJobDetails().add(jobDetail);
		job.setStatus(JobStatus.COMPLETED);
		assertFalse(broadCastService.isJobStopped(job));
	}

	@Test
	public void isJobCompletedTestWithBlocked() {
		Job job = new Job();
		JobDetail jobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.BLOCKED);
		job.getJobDetails().add(jobDetail);
		assertFalse(broadCastService.isJobCompleted(job, jobDetail));
	}

	@Test
	public void isJobCompletedTest() {
		Job job = new Job();
		JobDetail jobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.COMPLETED);
		job.getJobDetails().add(jobDetail);
		assertTrue(broadCastService.isJobCompleted(job, jobDetail));
	}

	@Test
	public void isJobCompletedTestWithMultipleJobDetail() {
		Job job = new Job();
		JobDetail jobDetail = new JobDetail();
		JobDetail lastJobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.COMPLETED);
		lastJobDetail.setStatus(CommandStatus.COMPLETED);
		job.getJobDetails().add(jobDetail);
		assertTrue(broadCastService.isJobCompleted(job, jobDetail));
	}

	@Test
	public void startJobCommandOperationsTest() throws LocalizedException, VersionParameterValueException {
		JobRunnerDetailCommand jobRunnerDetailCommand = new JobRunnerDetailCommand();
		JobCommand jobCommand = new JobCommand();
		DraftCommand draftCommand = new DraftCommand();
		jobCommand.setDraftCommand(draftCommand);
		JobDetail jobDetail = new JobDetail();
		jobDetail.setTestSet(new TestSet());
		jobCommand.setJobDetailId(jobDetail.getId());
		Job job = new Job();
		job.getJobDetails().add(jobDetail);
		when(jobRunnerDetailCommandService.getJobCommandByJobRunnerDetailCommand(jobRunnerDetailCommand))
				.thenReturn(jobCommand);
		when(jobDetailService.getById(jobCommand.getJobDetailId())).thenReturn(jobDetail);
		when(jobService.getById(jobDetail.getJobId())).thenReturn(job);
		broadCastService.startJobCommandOperations(jobRunnerDetailCommand);
		assertSame(CommandStatus.IN_PROGRESS, jobDetail.getStatus());
	}

	@Test
	public void startJobCommandOperationsTestWithCompleted() throws LocalizedException, VersionParameterValueException {
		Client client = new Client();
		JobRunnerDetailCommand jobRunnerDetailCommand = new JobRunnerDetailCommand();
		JobCommand jobCommand = new JobCommand();
		jobCommand.setCommandStatus(CommandStatus.COMPLETED);
		DraftCommand draftCommand = new DraftCommand();
		jobCommand.setDraftCommand(draftCommand);
		JobDetail jobDetail = new JobDetail();
		jobDetail.getJobCommands().add(jobCommand);
		jobDetail.setTestSet(new TestSet());
		jobDetail.setClient(client);
		jobDetail.setStatus(CommandStatus.IN_PROGRESS);
		jobCommand.setJobDetailId(jobDetail.getId());
		Job job = new Job();
		job.getJobDetails().add(jobDetail);
		when(jobRunnerDetailCommandService.getJobCommandByJobRunnerDetailCommand(jobRunnerDetailCommand))
				.thenReturn(jobCommand);
		when(jobDetailService.getById(jobCommand.getJobDetailId())).thenReturn(jobDetail);
		when(jobService.getById(jobDetail.getJobId())).thenReturn(job);
		broadCastService.startJobCommandOperations(jobRunnerDetailCommand);
		assertNull(jobDetail.getClient());
	}

}
