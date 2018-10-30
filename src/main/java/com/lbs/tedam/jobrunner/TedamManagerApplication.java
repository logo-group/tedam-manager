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

package com.lbs.tedam.jobrunner;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.lbs.tedam.data.config.DataConfig;
import com.lbs.tedam.data.service.impl.TedamUserServiceImpl;
import com.lbs.tedam.jobrunner.config.JobRunnerDataConfig;
import com.lbs.tedam.jobrunner.engine.JobRunnerEngine;
import com.lbs.tedam.jobrunner.manager.JobRunnerManager;
import com.lbs.tedam.jobrunner.rest.JobRunnerRestService;

@SpringBootApplication(scanBasePackageClasses = { DataConfig.class, JobRunnerDataConfig.class, TedamUserServiceImpl.class, JobRunnerManager.class, JobRunnerRestService.class,
		JobRunnerEngine.class })
public class TedamManagerApplication {

	public final static String REST_URL = "api";
	private static ConfigurableListableBeanFactory beanFactory;

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(TedamManagerApplication.class, args);
		beanFactory = context.getBeanFactory();
	}

	public static ConfigurableListableBeanFactory getBeanFactory() {
		return beanFactory;
	}

}
