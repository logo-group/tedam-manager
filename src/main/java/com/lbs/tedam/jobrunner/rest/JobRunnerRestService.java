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

package com.lbs.tedam.jobrunner.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.TedamManagerApplication;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.manager.JobRunnerManager;
import com.lbs.tedam.jobrunner.manager.JobRunnerScheduler;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.util.EnumsV2.ClientStatus;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamJsonFactory;

@RestController
@RequestMapping(TedamManagerApplication.REST_URL + "/JobRunnerRestService")
public class JobRunnerRestService implements HasLogger {

	private final JobRunnerManager jobRunnerManager;
	private final JobRunnerScheduler jobRunnerScheduler;
	private final ClientMapService clientMapService;
	private final JobService jobService;

	@Autowired
	public JobRunnerRestService(JobRunnerManager jobRunnerManager, ClientMapService clientMapService,
			JobService jobService, JobRunnerScheduler jobRunnerScheduler) {
		this.jobRunnerManager = jobRunnerManager;
		this.jobRunnerScheduler = jobRunnerScheduler;
		this.clientMapService = clientMapService;
		this.jobService = jobService;
	}

	@RequestMapping("/startJob")
	public String startJob(@RequestBody String jsonString) {
		Integer jobId = TedamJsonFactory.fromJson(jsonString, Integer.class);
		Job job;
		try {
			job = jobService.getById(jobId);
			jobRunnerScheduler.scheduleJob(job);
			getLogger().info("Job added to start. Name: " + job.getName());
			return HttpStatus.OK.getReasonPhrase();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
		}
	}

	@RequestMapping("/stopJob")
	public String stopJob(@RequestBody String jsonString) {
		Integer jobId = TedamJsonFactory.fromJson(jsonString, Integer.class);
		Job job;
		try {
			job = jobService.getById(jobId);
			jobRunnerManager.stopJob(job);
			getLogger().info("Job stop triggered. Name: " + job.getName());
			return HttpStatus.OK.getReasonPhrase();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
		}
	}

	@RequestMapping("/getClientMap")
	public String getClientMap() {
		Map<String, ClientStatus> clientMapString = clientMapService.getClientMapAsString();
		return TedamJsonFactory.toJson(clientMapString);

	}

}
