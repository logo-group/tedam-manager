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

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.lbs.tedam.data.service.PropertyService;
import com.lbs.tedam.data.service.SnapshotValueService;
import com.lbs.tedam.data.service.TedamScriptAccessorService;
import com.lbs.tedam.data.service.TestTimeLogService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.generator.steptype.Generator;
import com.lbs.tedam.generator.steptype.GeneratorFactory;
import com.lbs.tedam.generator.steptype.GridSearchGenerator;
import com.lbs.tedam.jobrunner.TedamManagerApplication;
import com.lbs.tedam.model.Property;
import com.lbs.tedam.model.SnapshotValue;
import com.lbs.tedam.model.TedamScriptAccessor;
import com.lbs.tedam.model.TestTimeLog;
import com.lbs.tedam.model.DTO.GridCell;
import com.lbs.tedam.recorder.TestStepTimeRecord;
import com.lbs.tedam.util.Constants;
import com.lbs.tedam.util.EnumsV2.ScriptAccessorOperationType;
import com.lbs.tedam.util.EnumsV2.ScriptAccessorType;
import com.lbs.tedam.util.EnumsV2.TestStepType;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamJsonFactory;

/*** Script rest service to do database operations. **/

@RestController
@RequestMapping(TedamManagerApplication.REST_URL + "/ScriptRestService")
public class ScriptRestService implements HasLogger {

	private final SnapshotValueService snapshotValueService;
	private final PropertyService propertyService;
	private final TedamScriptAccessorService tedamScriptAccessorService;
	private final TestTimeLogService testTimeLogService;
	private final BeanFactory beanFactory;

	@Autowired
	public ScriptRestService(SnapshotValueService snapshotValueService, PropertyService propertyService,
			TedamScriptAccessorService tedamScriptAccessorService, TestTimeLogService testTimeLogService,
			BeanFactory beanFactory) {
		this.snapshotValueService = snapshotValueService;
		this.propertyService = propertyService;
		this.tedamScriptAccessorService = tedamScriptAccessorService;
		this.testTimeLogService = testTimeLogService;
		this.beanFactory = beanFactory;

	}

	@RequestMapping("/getSnapshotFormFillValueBOList/{version}/{snapshotDefinitionId}")
	public String getSnapshotFormFillValueBOList(@PathVariable String version,
			@PathVariable String snapshotDefinitionId) {
		try {
			List<SnapshotValue> snapshotValueList = snapshotValueService.getSnapshotValuesVersioned(version,
					Integer.valueOf(snapshotDefinitionId), "RUN_ORDER, ROW_INDEX", false);
			getLogger().info("getSnapshotFormFillValueBOList size : " + snapshotValueList.size());
			return TedamJsonFactory.toJson(snapshotValueList);
		} catch (NumberFormatException | LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@RequestMapping("/getSnapshotFilterFillValueBOList/{version}/{snapshotDefinitionId}")
	public String getSnapshotFilterFillValueBOList(@PathVariable String version,
			@PathVariable String snapshotDefinitionId) {
		try {
			List<SnapshotValue> snapshotValueList = snapshotValueService.getSnapshotValuesVersioned(version,
					Integer.valueOf(snapshotDefinitionId), "RUN_ORDER", true);
			getLogger().info("getSnapshotFilterFillValueBOList size : " + snapshotValueList.size());
			return TedamJsonFactory.toJson(snapshotValueList);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@RequestMapping("/getGridSplitterIndex/{snapshotDefinitionId}/{tag}")
	public String getGridSplitterIndex(@PathVariable String snapshotDefinitionId, @PathVariable String tag) {
		try {
			Property property = propertyService.getPropertyByNameAndParameter(Constants.PROPERTY_SPLITTER,
					snapshotDefinitionId + "," + tag);
			getLogger().info("getGridSplitterIndex size : " + property);
			return TedamJsonFactory.toJson(property);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@RequestMapping("/getSnapshotValueList/{snapshotDefinitionId}")
	public String getSnapshotValueList(@PathVariable String snapshotDefinitionId) {
		try {
			List<SnapshotValue> snapshotValueList = snapshotValueService
					.getSnapshotValueList(Integer.valueOf(snapshotDefinitionId), -3);
			getLogger().info("getSnapshotValueList size : " + snapshotValueList.size());
			return TedamJsonFactory.toJson(snapshotValueList);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@RequestMapping("/getTedamScriptAccessorList/{scriptAccessorTypeValue}/{scriptAccessorOperationTypeValue}")
	public String getTedamScriptAccessorList(@PathVariable String scriptAccessorTypeValue,
			@PathVariable String scriptAccessorOperationTypeValue) {
		try {
			List<TedamScriptAccessor> tedamScriptAccessorList = tedamScriptAccessorService
					.getTedamScriptAccessorListByScriptAndOperationType(
							ScriptAccessorType.fromValue(scriptAccessorTypeValue),
							ScriptAccessorOperationType.fromValue(scriptAccessorOperationTypeValue));
			getLogger().info("getTedamScriptAccessorList size : " + tedamScriptAccessorList.size());
			return TedamJsonFactory.toJson(tedamScriptAccessorList);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@RequestMapping("/getGridSearchParameterList/**")
	public String getGridSearchParameterList(HttpServletRequest request) {
		try {
			String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
			String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			AntPathMatcher apm = new AntPathMatcher();
			String gridSearchParameter = apm.extractPathWithinPattern(bestMatchPattern, path);
			Generator generator = GeneratorFactory.getGenerator(TestStepType.GRID_SEARCH, beanFactory);
			generator.degenerate(gridSearchParameter);
			List<GridCell> list = ((GridSearchGenerator) generator).getSearchValues();
			return TedamJsonFactory.toJson(list);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/saveTestStepTimeRecordList")
	public void saveTestStepTimeRecordList(@RequestBody String jsonString) {
		try {
			List<TestStepTimeRecord> timeRecordList = TedamJsonFactory.fromJsonList(jsonString,
					TestStepTimeRecord.class);
			getLogger().info("saveTestStepTimeRecordList size : " + timeRecordList.size());
			List<TestTimeLog> convertedList = testTimeLogService.convertTimeRecord2TestTimeLog(timeRecordList);
			testTimeLogService.save(convertedList);
		} catch (Exception e) {
			getLogger().error(e.getLocalizedMessage(), e);
		}
	}

}
