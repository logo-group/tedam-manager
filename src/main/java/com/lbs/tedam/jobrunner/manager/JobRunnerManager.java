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

package com.lbs.tedam.jobrunner.manager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.data.service.ProjectService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.engine.JobRunnerEngine;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.Project;
import com.lbs.tedam.util.HasLogger;

@Component
public class JobRunnerManager implements Serializable, HasLogger {

	/** long serialVersionUID */
	private static final long serialVersionUID = 1L;

	private Map<Project, JobRunnerEngine> projectEngineMap = new HashMap<>();

	private final ProjectService projectService;
	private final JobService jobService;
	private final ClientMapService clientMapService;
	private final BeanFactory beanFactory;

	@Autowired
	public JobRunnerManager(ProjectService projectService, ClientMapService clientMapService, JobService jobService, BeanFactory beanFactory) {
		this.projectService = projectService;
		this.beanFactory = beanFactory;
		this.clientMapService = clientMapService;
		this.jobService = jobService;
	}

	@PostConstruct
	public void init() throws LocalizedException {
		clientMapService.buildClientMap();
		buildProjectEngineMap();
	}

	private void buildProjectEngineMap() throws LocalizedException {
		List<Project> projectList = projectService.getProjectList();
		for (Project project : projectList) {
			JobRunnerEngine jobRunnerEngine = beanFactory.getBean(JobRunnerEngine.class);
			jobRunnerEngine.setProjectName(project.getName());
			Thread thread = new Thread(jobRunnerEngine, project.getName() + "-Thread");
			thread.start();
			projectEngineMap.put(project, jobRunnerEngine);
		}
	}

	public void addJob(Job tempJob) throws LocalizedException {
		Job job = jobService.getById(tempJob.getId());
		job.setJobGroupId(tempJob.getJobGroupId());
		JobRunnerEngine jobRunnerEngine = projectEngineMap.get(job.getProject());
		// job.setProject(getProjectFromMap(job.getProject()));
		jobRunnerEngine.addNewJobToQueue(job);
	}

	// public Project getProjectFromMap(Project tempProject) {
	// for (Project project : projectEngineMap.keySet()) {
	// if (project.equals(tempProject)) {
	// return project;
	// }
	// }
	// return null;
	// }

	public List<Job> getQueuedJobs(Project project) {
		JobRunnerEngine jobRunnerEngine = projectEngineMap.get(project);
		return jobRunnerEngine.getQueuedJobs();
	}

	public void removeJobFromRunningJobs(Job job) {
		JobRunnerEngine jobRunnerEngine = projectEngineMap.get(job.getProject());
		jobRunnerEngine.removeJobFromRunningJobs(job);
	}

	public void stopJob(Job job) {
		JobRunnerEngine jobRunnerEngine = projectEngineMap.get(job.getProject());
		jobRunnerEngine.removeJobFromQueue(job);
		jobRunnerEngine.removeJobFromRunningJobs(job);
		jobRunnerEngine.setJobRemoveQueue(true);
	}

	public Map<Project, JobRunnerEngine> getProjectEngineMap() {
		return projectEngineMap;
	}

	public void setProjectEngineMap(Map<Project, JobRunnerEngine> projectEngineMap) {
		this.projectEngineMap = projectEngineMap;
	}

}
