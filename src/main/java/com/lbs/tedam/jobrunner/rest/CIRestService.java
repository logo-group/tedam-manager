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

import java.util.List;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.TedamManagerApplication;
import com.lbs.tedam.jobrunner.service.CIRestServiceController;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.Project;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamProcessUtils;

/*** Continuous integration rest service to run jobs. **/

@RestController
@RequestMapping(TedamManagerApplication.REST_URL + "/CIRestService")
public class CIRestService implements HasLogger {

	private final CIRestServiceController restServiceController;

	@Autowired
	public CIRestService(CIRestServiceController restServiceController) {
		this.restServiceController = restServiceController;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/runCIJobs")
	public Response runCIJobs(@QueryParam("projectName") String projectName) {
		if (!StringUtils.isEmpty(projectName)) {
			Project project = null;
			List<Job> jobList = null;
			try {
				project = restServiceController.getProject(projectName);
				jobList = restServiceController.collectJobList(project);
				restServiceController.addJobsToQueue(jobList);
			} catch (LocalizedException e) {
				getLogger().error(e.getLocalizedMessage(), e);
				return restServiceController.createErrorResponse();
			}
			while (restServiceController.checkRunningCIJobs(jobList, project)) {
				getLogger().info("jobs are going on");
				TedamProcessUtils.sleepThread(5000);
			}
			return restServiceController.createOkResponse();
		} else
			return restServiceController.createErrorResponse();
	}

}
