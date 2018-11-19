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

package com.lbs.tedam.jobrunner.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.lbs.tedam.data.service.JobRunnerCommandService;
import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.exception.JobCommandBuildException;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.service.JobRunnerEngineService;
import com.lbs.tedam.jobrunner.websocket.server.JobRunnerSocketServerListener;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobDetail;
import com.lbs.tedam.model.JobRunnerCommand;
import com.lbs.tedam.util.EnumsV2.CommandStatus;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamProcessUtils;

@Component
@Scope("prototype")
public class JobRunnerEngine implements Runnable, HasLogger {

	private String projectName;

	public static final int sleepWaitMillis = 50;

	/** List<Job> queuedJobs is list of jobs held in the queue */
	private List<Job> queuedJobs = new ArrayList<>();

	/** Queue<JobDetail> jobQueue is queue structure of jobDetails of jobs received in the queue */
	private Deque<JobDetail> jobDetailQueue = new ArrayDeque<>();

	/** List<Job> runningJobs list of running jobs */
	private List<Job> runningJobs = new ArrayList<>();

	/** boolean isJobRemoveQueue is if a job is stopped or deleted, we should delete the job from the queue */
	private boolean isJobRemoveQueue;

	private final JobRunnerEngineService jobRunnerEngineService;
	private final ClientMapService clientMapService;
	private final JobRunnerSocketServerListener jobRunnerSocketServerListener;
	private final JobRunnerCommandService jobRunnerCommandService;
	private final JobService jobService;

	@Autowired
	public JobRunnerEngine(JobRunnerEngineService jobRunnerEngineService, ClientMapService clientMapService, JobRunnerSocketServerListener jobRunnerSocketServerListener,
			JobRunnerCommandService jobRunnerCommandService, JobService jobService) {
		this.jobRunnerEngineService = jobRunnerEngineService;
		this.clientMapService = clientMapService;
		this.jobRunnerSocketServerListener = jobRunnerSocketServerListener;
		this.jobRunnerCommandService = jobRunnerCommandService;
		this.jobService = jobService;
	}

	@Override
	public void run() {
		getLogger().info(projectName + " - JobRunnerEngine starting...");
		try {
			engineStart();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * this method engineStart is main procedure for managing the queue and clients <br>
	 * @author Tarik.Mikyas <br>
	 * @throws LocalizedException 
	 */
	public void engineStart() throws LocalizedException {
		JobDetail jobDetail = null;
		isJobRemoveQueue = false;
		/** List<JobDetail> runningJobDetailList running jobDetails */
		while (true) {
			try {
				// planned, historic jobs are drawn from all planned jobs and added to the list
				if (jobDetail == null) {
					jobDetail = jobDetailQueue.peek(); // addNewJobToQueue() Added to the queue is the jobDetail at the top of the queue.
					if (jobDetail != null) {
						getLogger().info("jobDetail (peek) pulled . jobDetail.getId() :" + jobDetail.getId() + "  testSetId : " + jobDetail.getTestSet().getId());
					}
				}
				if (isJobRemoveQueue) { // If the job is stopped or deleted, the last jobDetail handled is also null. Then isJobRemoveQueue is again false.
					getLogger().info("job is stopped or deleted, the last jobDetail handled is also nullified. isJobRemoveQueue : " + isJobRemoveQueue);
					jobDetail = null;
					isJobRemoveQueue = false;
					getLogger().info("The last jobDetail handled by the job was stopped or deleted. isJobRemoveQueue false pulled: " + isJobRemoveQueue);
				}
				// if you have a jobDetail and you do not run the jobDetail's parent
				if (jobDetail != null) {
					if (checkJobDetail(jobDetail)) {
						// If testSet or testCase of jobDetail is empty, then jobDetail is deleted and removed from jobDetailQueue, jobDetail is null.
						jobRunnerEngineService.removeJobDetailsAndUpdateJob(jobDetail);
						jobDetailQueue.poll();
						jobDetail = null;
					} else {
						Job job = jobService.getById(jobDetail.getJobId());
						// this jobDetail's job status paused should be checked. If paused, jobDetail is null.
						getLogger().debug(
								jobDetail.getTestSet().getId() + " testSetId li " + jobDetail.getId() + " for jobdetailled jobdetail the method of getAvailableClient is called");
						Client client = clientMapService.getAvaliableClient(job);
						// bring back the first available client
						if (client != null) { // If client is found for jobDetail
							getLogger().info(
									"getAvaliableClient(" + job.getName() + ") metodu cagirildi. clientName : " + client.getName() + " clientId : " + client.getId() + " found.");
							jobDetailQueue.poll(); // After the client is found, the object at the top of the queue is disconnected from the queue.
							getLogger().info("jobDetailQueue from pool with job detail : " + jobDetail.getId() + " , testSetId: " + jobDetail.getTestSet().getId() + " severed.");
							jobDetail.setClient(client);
							getLogger().info(client.getName() + " client " + jobDetail.getTestSet().getId() + " testSetId li " + jobDetail.getId() + " jobDetailId setend.");
							try {
								getLogger().info(
										"jobDetailId : " + jobDetail.getId() + "clientId : " + jobDetail.getClient().getId() + " clientName : " + jobDetail.getClient().getName());
								jobDetail = jobRunnerEngineService.setJobDetailAttributes(jobDetail);
								if (!runningJobs.contains(job)) {
									runningJobs.add(job);
									getLogger().info("We have a job detail. Client was found. RunningJobs was added to runtime jobs. ");
								}
	
								getLogger().info("jobDetailId : " + jobDetail.getId() + " clientId : " + jobDetail.getClient().getId() + " command will be sent.");
								sendJobDetailCommand(jobDetail);
								getLogger().info("jobDetailId : " + jobDetail.getId() + " clientId : " + jobDetail.getClient().getId() + " command was sent.");
	
							} catch (JobCommandBuildException e) {
								getLogger().error("jobDetailId : " + jobDetail.getId() + " Error occurred while creating JobCommand. " + e);
							} catch (NullPointerException e) {
								getLogger().error("jobDetailId : " + jobDetail.getId() + " The client can not be set in jobDetail. jobDetail.getClient() : " + jobDetail.getClient() + " " + e);
								// updateMap(client, ClientStatus.FREE);
							} catch (Exception e) {
								getLogger().error("UNKNOWN ERROR. jobDetailId : " + jobDetail.getId() + " for " + e);
							}
							jobDetail = null;
							// TODO:canberk it has to be parametric.
							TedamProcessUtils.sleepThread(sleepWaitMillis);
							// If there is no client for jobDetail, then all other jobs under that jobDetail in job must be dropped at the end of the queue
							// if the number of jobs in the queue is more than one and
							// If any of the jobdetail's job's clients do not have a static clientMap, and
							// all the jobs in the queue should be thrown to the end of the queue (jobDetailQueue) if any of the clients of the client are available
						} else if (queuedJobs.size() > 1 && !clientMapService.isJobClientAvailableAtClientMap(job) && isAnyClientAvailableAtOtherJobs(job)) {
							getLogger().info(jobDetail.getId() + "  testSetId : " + jobDetail.getTestSet().getId() + " to be sent to the end of the queue.");
							sendUnRunningJobDetailsEndOfQueue(job); // jobDetailers who will not occupy the queue and will be occupied by the queue should be sent to the end of the queue.
							getLogger().info(jobDetail.getId() + "  testSetId : " + jobDetail.getTestSet().getId() + " The end of the queue has been sent for.");
							jobDetail = null;
							TedamProcessUtils.sleepThread(sleepWaitMillis);
						}
					}
				}
				TedamProcessUtils.sleepThread(sleepWaitMillis);
			} catch (Exception e) {
				getLogger().error(e.getLocalizedMessage(), e);
			}
		}
	}

	private void sendJobDetailCommand(JobDetail jobDetail) {
		JobRunnerCommand jobRunnerCommand = jobRunnerCommandService.createJobRunnerCommand(jobDetail);
		jobRunnerSocketServerListener.sendJobRunnerCommand(jobRunnerCommand);
	}

	/**
	 * this method checkJobDetail is check whether the testSet and testCase of jobDetail are empty. if it is empty returns true <br>
	 * @author Canberk.Erkmen
	 * @param jobDetail
	 * @return <br>
	 * @throws LocalizedException 
	 */
	private boolean checkJobDetail(JobDetail jobDetail) throws LocalizedException {
		if (jobRunnerEngineService.isJobJobDetailEmpty(jobDetail)) {
			return true;
		}
		return false;
	}

	/**
	 * From running pools to running jobs <br>
	 * does anybody have a static clientMap from the clients in the pool? if there is, it returns true else it returns false <br>
	 * If there is no pool and if client job(s) well clientmap any non-static pool returns true. <br>
	 * @author Tarik.Mikyas
	 * @return <br>
	 */
	private boolean isAnyClientAvailableAtOtherJobs(Job controlledJob) {
		for (Job job : queuedJobs) { // Will visit all jobs running on the system
			if (job.equals(controlledJob)) { // The attempted removal of the last job in the queue in jobdetail we should not need to check again. Its not already clients of suitable
				continue;
			}
			if (clientMapService.isJobClientAvailableAtClientMap(job)) { // If any is the pool
				getLogger().info("the job in the queue " + job.getName() + "  client's suitable for the job.");
				return true;
			} else if (job.getClients() == null && clientMapService.getAvaliableClientForWithoutClientPool(job, false) != null) { // eger havuzsuz job ise
				getLogger().info("the job in the queue " + job.getName() + "  client's suitable for the job.");
				return true;
			}
		}
		return false;
	}

	/**
	 * this method sendUnruningJobDetailsEndOfQueue is the client status is unsuitable because of the busy queue for that jobDetailListe <br>
	 * Each element is sent to the end of the queue. <br>
	 * @author Tarik.Mikyas
	 * @param ranJobDetailList
	 *            <br>
	 */
	private void sendUnRunningJobDetailsEndOfQueue(Job job) {
		for (JobDetail tempJobDetail : job.getJobDetails()) { // from jobdDetail's job's visit is above all joDetail
			JobDetail queueJobDetail = jobDetailQueue.peek(); // jobdetail first in the queue are handled.
			if (tempJobDetail.getJobId().equals(queueJobDetail.getJobId())) { // if the tail, which is the same as those of the entire list of job jobdetail
				jobDetailQueue.poll(); // We tearing the beginning of the tail
				jobDetailQueue.add(queueJobDetail); // We are adding to the end of the queue
			}
		}
	}

	/**
	 * Pressing the Dashboard to start <br>
	 * or planneddat i have come to be a part of routine job when called.<br>
	 * @author Tarik.Mikyas
	 * @param job
	 *            <br>
	 */
	public void addNewJobToQueue(Job job) {
		addJobToQueuedJobs(job);
		for (JobDetail jobDetail : job.getJobDetails()) {
			//If the situation jobDetail NOT_STARTED (unable to start) must be added to the queue. Completed and Started up again running.
			// When adding a queue jobdetail one-sided relationship between the need to make mutually in the job.
			if (CommandStatus.NOT_STARTED == jobDetail.getStatus()) {
				// jobDetail.setJob(job);
				jobDetailQueue.add(jobDetail);
			}
		}
		getLogger().info("jobId :" + job.getId() + " belonging to +" + job.getJobDetails().size() + " jobdetail one was added to the queue.");
	}

	/**
	 * this method addJobToQueuedJobs is jobdetail be placed in the queue jobdetail 'adds the job to the job queue <br>
	 * @author Tarik.Mikyas <br>
	 */
	private void addJobToQueuedJobs(Job job) {
		if (!queuedJobs.contains(job)) {
			queuedJobs.add(job);
			getLogger().info("jobDetailQueue jobdetail of queued jobs jobqueu added to the queue.");
		}
	}

	/**
	 * this method updateRunningJobs is If delete the given job runningjobs<br>
	 * @author Tarik.Mikyas
	 * @param job
	 *            <br>
	 */
	public void removeJobFromRunningJobs(Job job) {
		if (runningJobs.contains(job)) {
			runningJobs.remove(job);
			queuedJobs.remove(job);
		}
	}

	/**
	 * this method isJobRemoveQueue <br>
	 * @author Tarik.Mikyas
	 * @return <br>
	 */
	public boolean isJobRemoveQueue() {
		return isJobRemoveQueue;
	}

	/**
	 * this method setJobRemoveQueue <br>
	 * @author Tarik.Mikyas
	 * @param isJobRemoveQueue
	 *            <br>
	 */
	public void setJobRemoveQueue(boolean isJobRemoveQueue) {
		this.isJobRemoveQueue = isJobRemoveQueue;
	}

	/**
	 * this method getRunningJobs <br>
	 * @author Canberk.Erkmen
	 * @return <br>
	 */
	public List<Job> getRunningJobs() {
		return runningJobs;
	}

	/**
	 * jobDetailQueue is It used to delete the job.
	 * 
	 * @author Ahmet.Izgi
	 * @param job
	 */
	public void removeJobFromQueue(Job job) {
		for (JobDetail jobDetail : job.getJobDetails()) {
			jobDetailQueue.remove(jobDetail);
		}
	}

	/**
	 * @return the queuedJobs
	 */
	public List<Job> getQueuedJobs() {
		return queuedJobs;
	}

	@Override
	public String toString() {
		return projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Deque<JobDetail> getJobDetailQueue() {
		return jobDetailQueue;
	}

	public void setJobDetailQueue(Deque<JobDetail> jobDetailQueue) {
		this.jobDetailQueue = jobDetailQueue;
	}

	@Override
	public Logger getLogger() {
		return LoggerFactory.getLogger(projectName + " " + getClass());
	}
}
