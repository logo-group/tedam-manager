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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.engine.JobRunnerEngine;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamProcessUtils;

@Component
public class JobRunnerScheduler implements Serializable, HasLogger, Runnable {

	/** long serialVersionUID */
	private static final long serialVersionUID = 1L;

	private final JobRunnerManager jobRunnerManager;
	private final List<Job> jobPool = new ArrayList<>();
	private Thread schedularThread = new Thread(this, "JobRunnerScheduler");

	@Autowired
	public JobRunnerScheduler(JobRunnerManager jobRunnerManager) {
		this.jobRunnerManager = jobRunnerManager;
	}

	@PostConstruct
	public void init() throws LocalizedException {
		schedularThread.start();
	}

	public void scheduleJob(Job job) throws LocalizedException {
		LocalDateTime plannedDate = job.getPlannedDate();
		if (plannedDate != null) {
			jobPool.add(job);
			getLogger().info("Job scheduled(ID, NAME): " + job.getId() + " - " + job.getName());
		} else {
			jobRunnerManager.addJob(job);
		}
	}

	public void stopJob(Job job) throws LocalizedException {
		if (jobPool.contains(job)) {
			jobPool.remove(job);
		}
		jobRunnerManager.stopJob(job);
	}

	@Override
	public void run() {
		while (true) {
			Iterator<Job> iterator = jobPool.iterator();
			while (iterator.hasNext()) {
				Job job = iterator.next();
				if (LocalDateTime.now().compareTo(job.getPlannedDate()) <= 0) {
					try {
						jobRunnerManager.addJob(job);
						iterator.remove();
						getLogger().info("Job added to manager and removed from pool(ID, NAME): " + job.getId() + " - "
								+ job.getName());
					} catch (LocalizedException e) {
						getLogger().error(e.getLocalizedMessage(), e);
					}
				}
			}
			TedamProcessUtils.sleepThread(JobRunnerEngine.sleepWaitMillis);
		}
	}

}
