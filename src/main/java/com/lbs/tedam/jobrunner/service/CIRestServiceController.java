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

import java.util.List;

import javax.ws.rs.core.Response;

import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.Project;

/**
 * Continous integration rest service interface.
 */
public interface CIRestServiceController {

	/**
	 * this method collectJobList <br>
	 * 
	 * @author Canberk.Erkmen
	 * @param project
	 * @return <br>
	 * @throws LocalizedException
	 */
	public List<Job> collectJobList(Project project) throws LocalizedException;

	/**
	 * Creates ok response.
	 * 
	 * @return Response that is ok.
	 */
	public Response createOkResponse();

	/**
	 * Creates error response.
	 * 
	 * @return Response that is error.
	 */
	public Response createErrorResponse();

	/**
	 * this method checkRunningCIJobs <br>
	 * 
	 * @author Canberk.Erkmen
	 * @param jobList
	 * @return <br>
	 */
	public boolean checkRunningCIJobs(List<Job> jobList, Project project);

	/**
	 * this method addJobsToQueue <br>
	 * 
	 * @author Canberk.Erkmen
	 * @param jobList <br>
	 * @throws LocalizedException
	 */
	public void addJobsToQueue(List<Job> jobList) throws LocalizedException;

	/**
	 * Finds the project given by project name.
	 * 
	 * @param projectName Project name to find project.
	 * @return If projectName is valid project instance else null.
	 * @throws LocalizedException
	 */
	public Project getProject(String projectName) throws LocalizedException;

}
