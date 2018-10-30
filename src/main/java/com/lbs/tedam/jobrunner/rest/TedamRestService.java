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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lbs.tedam.jobrunner.TedamManagerApplication;
import com.lbs.tedam.data.service.PropertyService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamFileUtils;
import com.lbs.tedam.util.TedamJsonFactory;

/**
 * Script rest service to do tedam operations
 *
 */
@RestController
@RequestMapping(TedamManagerApplication.REST_URL + "/TedamRestService")
public class TedamRestService implements HasLogger {

	private final PropertyService propertyService;

	@Autowired
	public TedamRestService(PropertyService propertyService) {
		this.propertyService = propertyService;
	}

	@RequestMapping("/getFileContent/{testCaseId}/{fileName}")
	public String getFileContent(@PathVariable String testCaseId, @PathVariable String fileName) {
		String filePath;
		try {
			filePath = propertyService.getTestcaseFolder(Integer.valueOf(testCaseId)) + fileName + ".bsh";
			String fileContent = TedamFileUtils.getFileContent(filePath);
			return TedamJsonFactory.toJson(fileContent);
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

}
