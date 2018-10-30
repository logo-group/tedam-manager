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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;

import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.data.service.ProjectService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.engine.JobRunnerEngine;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobDetail;
import com.lbs.tedam.model.Project;
import com.lbs.tedam.util.EnumsV2.CommandStatus;

public class JobRunnerManagerTest {

	private ProjectService projectService;
	private JobService jobService;
	private ClientMapService clientMapService;
	private BeanFactory beanFactory;
	private JobRunnerManager jobRunnerManager;
	private JobRunnerEngine jobRunnerEngine;
	Map<Project, JobRunnerEngine> projectEngineMap;

	@Before
	public void beforeTest() {
		projectService = mock(ProjectService.class);
		jobService = mock(JobService.class);
		clientMapService = mock(ClientMapService.class);
		beanFactory = mock(BeanFactory.class);
		projectEngineMap = mock(HashMap.class);
		jobRunnerEngine = mock(JobRunnerEngine.class);
		jobRunnerManager = new JobRunnerManager(projectService, clientMapService, jobService, beanFactory);
	}

	@Test
	public void buildProjectEngineMapTestWithEmptyList() throws LocalizedException {
		List<Project> projectList = new ArrayList<>();
		when(projectService.getProjectList()).thenReturn(projectList);
		jobRunnerManager.init();
		assertTrue(jobRunnerManager.getProjectEngineMap().isEmpty());
	}

	@Test
	public void buildProjectEngineMapTest() throws LocalizedException {
		List<Project> projectList = new ArrayList<>();
		Project project = new Project();
		project.setName("TEDAM");
		projectList.add(project);
		when(projectService.getProjectList()).thenReturn(projectList);
		JobRunnerEngine jobRunnerEngine = new JobRunnerEngine(null, null, null, null, null);
		when(beanFactory.getBean(JobRunnerEngine.class)).thenReturn(jobRunnerEngine);
		jobRunnerManager.init();
		assertTrue(!jobRunnerManager.getProjectEngineMap().isEmpty());
	}

	@Test
	public void addJobTest() throws LocalizedException {
		Job job = new Job();
		job.setId(10);
		Project project = new Project();
		job.setProject(project);
		JobDetail jobDetail = new JobDetail();
		jobDetail.setStatus(CommandStatus.NOT_STARTED);
		job.getJobDetails().add(jobDetail);
		JobRunnerEngine jobRunnerEngine = new JobRunnerEngine(null, null, null, null, null);
		jobRunnerManager.getProjectEngineMap().put(project, jobRunnerEngine);
		when(jobService.getById(job.getId())).thenReturn(job);
		jobRunnerManager.addJob(job);
	}

	@Test
	public void getQueuedJobsTest() {
		Project project = new Project();
		JobRunnerEngine jobRunnerEngine = new JobRunnerEngine(null, null, null, null, null);
		jobRunnerManager.getProjectEngineMap().put(project, jobRunnerEngine);
		assertTrue(jobRunnerManager.getQueuedJobs(project).isEmpty());
	}

	@Test
	public void removeJobFromRunningJobsTest() {
		Job job = new Job();
		Project project = new Project();
		job.setProject(project);
		JobRunnerEngine jobRunnerEngine = new JobRunnerEngine(null, null, null, null, null);
		jobRunnerManager.getProjectEngineMap().put(project, jobRunnerEngine);
		jobRunnerManager.removeJobFromRunningJobs(job);
		assertTrue(!jobRunnerEngine.getRunningJobs().contains(job));
	}

	@Test
	public void stopJobTest() {
		Job job = new Job();
		Project project = new Project();
		job.setProject(project);
		JobRunnerEngine jobRunnerEngine = new JobRunnerEngine(null, null, null, null, null);
		jobRunnerManager.getProjectEngineMap().put(project, jobRunnerEngine);
		jobRunnerManager.stopJob(job);
		assertTrue(!jobRunnerEngine.getRunningJobs().contains(job));
	}

	@Test
	public void setProjectEngineMapTest() {
		jobRunnerManager.setProjectEngineMap(projectEngineMap);
		assertSame(projectEngineMap, jobRunnerManager.getProjectEngineMap());
	}
}
