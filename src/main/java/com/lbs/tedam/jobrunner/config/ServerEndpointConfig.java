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

package com.lbs.tedam.jobrunner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.lbs.tedam.data.service.ClientService;
import com.lbs.tedam.data.service.DraftCommandService;
import com.lbs.tedam.data.service.JobCommandService;
import com.lbs.tedam.data.service.JobDetailService;
import com.lbs.tedam.data.service.JobParameterService;
import com.lbs.tedam.data.service.JobRunnerDetailCommandService;
import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.data.service.ProjectService;
import com.lbs.tedam.data.service.PropertyService;
import com.lbs.tedam.data.service.TestCaseService;
import com.lbs.tedam.data.service.TestSetService;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.manager.JobRunnerManager;
import com.lbs.tedam.jobrunner.service.BroadcastService;
import com.lbs.tedam.jobrunner.service.CIRestServiceController;
import com.lbs.tedam.jobrunner.service.JobRunnerEngineService;
import com.lbs.tedam.jobrunner.service.impl.BroadcastServiceImpl;
import com.lbs.tedam.jobrunner.service.impl.CIRestServiceControllerImpl;
import com.lbs.tedam.jobrunner.service.impl.JobRunnerEngineServiceImpl;
import com.lbs.tedam.jobrunner.websocket.server.JobRunnerSocketServer;
import com.lbs.tedam.jobrunner.websocket.server.JobRunnerSocketServerListener;

@Configuration
public class ServerEndpointConfig {

	@Bean
	public BroadcastService broadcastService(ClientMapService clientMapService, JobService jobService, JobCommandService jobCommandService, JobDetailService jobDetailService,
			JobRunnerManager jobRunnerManager, JobRunnerDetailCommandService jobRunnerDetailCommandService, PropertyService propertyService,
			JobParameterService jobParameterService, TestCaseService testCaseService) {
		return new BroadcastServiceImpl(jobRunnerManager, clientMapService, jobService, jobCommandService, jobDetailService, jobRunnerDetailCommandService, propertyService);
	}

	@Bean
	public CIRestServiceController ciRestServiceController(JobService jobService, ProjectService projectService, JobRunnerManager jobRunnerManager) {
		return new CIRestServiceControllerImpl(jobService, projectService, jobRunnerManager);
	}

	@Bean
	public JobRunnerEngineService jobRunnerEngineService(JobService jobService, JobDetailService jobDetailService, TestSetService testSetService, TestCaseService testCaseService,
			DraftCommandService draftCommandService, JobParameterService jobParameterService) {
		return new JobRunnerEngineServiceImpl(jobService, jobDetailService, testSetService, testCaseService, draftCommandService, jobParameterService);
	}

	@Bean
	public JobRunnerSocketServerListener jobRunnerSocketServerListener(BroadcastService broadcastService, ClientMapService clientMapService, ClientService clientService) {
		return new JobRunnerSocketServerListener(broadcastService, clientMapService, clientService);
	}

	@Bean
	public JobRunnerSocketServer jobRunnerSocketServer(JobRunnerSocketServerListener listener) {
		return new JobRunnerSocketServer(listener);
	}

	@Bean
	public ServerEndpointExporter endpointExporter() {
		return new ServerEndpointExporter();
	}
}
