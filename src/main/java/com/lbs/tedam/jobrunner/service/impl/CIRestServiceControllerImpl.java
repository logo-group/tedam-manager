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
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;

import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.data.service.ProjectService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.manager.JobRunnerManager;
import com.lbs.tedam.jobrunner.service.CIRestServiceController;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.Project;
import com.lbs.tedam.util.EnumsV2.CommandStatus;
import com.lbs.tedam.util.EnumsV2.JobStatus;
import com.lbs.tedam.util.HasLogger;

/**
 * Continous integration rest service interface implementation.
 */
public class CIRestServiceControllerImpl implements CIRestServiceController, HasLogger {

	private final JobService jobService;
	private final ProjectService projectService;
	private final JobRunnerManager jobRunnerManager;

	@Autowired
	public CIRestServiceControllerImpl(JobService jobService, ProjectService projectService,
			JobRunnerManager jobRunnerManager) {
		this.jobService = jobService;
		this.projectService = projectService;
		this.jobRunnerManager = jobRunnerManager;
	}

	@Override
	public List<Job> collectJobList(Project project) throws LocalizedException {
		List<Job> jobList = new ArrayList<>();
		if (project != null)
			jobList = jobService.getCIJobListByProject(project);
		return jobList;
	}

	@Override
	public Project getProject(String projectName) throws LocalizedException {
		Project project = projectService.getProjectList().stream()
				.filter(tempProject -> tempProject.getName().equals(projectName)).findAny().orElse(null);
		return project;
	}

	@Override
	public Response createOkResponse() {
		return Response.status(Status.OK).build();
	}

	@Override
	public Response createErrorResponse() {
		return Response.status(Status.PRECONDITION_FAILED).build();
	}

	@Override
	public boolean checkRunningCIJobs(List<Job> jobList, Project project) {
		for (Job job : jobRunnerManager.getQueuedJobs(project)) {
			if (jobList.contains(job)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void addJobsToQueue(List<Job> jobList) throws LocalizedException {
		for (Job job : jobList) {
			switch (job.getStatus()) {
			case COMPLETED:
			case NOT_STARTED:
			case STOPPED:
				jobService.saveJobAndJobDetailsStatus(job, JobStatus.QUEUED, CommandStatus.NOT_STARTED, null);
				jobRunnerManager.addJob(job);
				break;
			default:
				break;
			}
		}
	}

}
