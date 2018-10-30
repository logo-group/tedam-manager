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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lbs.tedam.data.service.ClientService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.util.EnumsV2.ClientStatus;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamJsonFactory;

@Service
public class ClientMapService implements Serializable, HasLogger {

	/** long serialVersionUID */
	private static final long serialVersionUID = 1L;

	private Map<Client, ClientStatus> clientMap = new HashMap<>();

	private final ClientService clientService;

	@Autowired
	public ClientMapService(ClientService clientService) {
		this.clientService = clientService;
	}

	public void buildClientMap() throws LocalizedException {
		List<Client> clientList = clientService.getClientList();
		clientList.forEach(client -> clientMap.put(client, ClientStatus.DEAD));
	}

	public void updateClientMap(Client client, ClientStatus clientStatus) {
		if(client != null) {
			clientMap.put(client, clientStatus);
		}
	}

	/**
	 * Checks for if any free client in map<br>
	 * @author Tarik.Mikyas
	 * @param job
	 * @return <br>
	 */
	public Client getAvaliableClient(Job job) {
		Client availableClient = null;
		// If any one client in the pool and it jobclient equal to one client that will bring him clientmap.
		if (job.getClients() != null && job.getClients().size() > 0) {
			for (Map.Entry<Client, ClientStatus> entry : clientMap.entrySet()) {
				if (entry.getKey().getProject().equals(job.getProject()) && job.getClients().contains(entry.getKey()) //
						&& ClientStatus.FREE.equals(entry.getValue())) {
					clientMap.put(entry.getKey(), ClientStatus.BUSY);
					getLogger().info(job.getName() + " pool for job " + entry.getKey().getName() + " client is found. value :" + entry.getValue().getValue() + " set.");
					availableClient = entry.getKey();
					break;
				}
			}
		} else {//If there is no pool, it gets the first available client.
			availableClient = getAvaliableClientForWithoutClientPool(job, true);
		}
		return availableClient;
	}

	/**
	 * this method getAvaliableClientForWithoutClientPool is If there is no pool, when searching for available clients <br>
	 * If there are jobs in the queue from the pool <br>
	 * Without this pool as a parameter <br>
	 * those that are available to clients of other pooled jobs on the tail must not return<br>
	 * It is available to clients who are not located in any pool.<br>
	 * @author Tarik.Mikyas
	 * @param job
	 * @param willStatusSet
	 *            if true clientMap is updated, if false it is not updated
	 * @return <br>
	 */
	public Client getAvaliableClientForWithoutClientPool(Job job, boolean willStatusSet) {
		for (Map.Entry<Client, ClientStatus> entry : clientMap.entrySet()) {
			if (entry.getKey().getProject().equals(job.getProject()) && ClientStatus.FREE.equals(entry.getValue())) {
				getLogger().info(job.getName() + " for without pool " + entry.getKey().getName() + " client is found.");
				if (willStatusSet) {
					clientMap.put(entry.getKey(), ClientStatus.BUSY);
					getLogger().info(job.getName() + " for without pool " + entry.getKey().getName() + " client is found. value :" + entry.getValue() + " set.");
				}
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * this method isJobClientAvailableAtClientMap Is there any available in the static clientMap of the given job's clients <br>
	 * if there is returns true, if not available returns false <br>
	 * @author Tarik.Mikyas
	 * @param job
	 * @param clientMap
	 * @return <br>
	 */
	public boolean isJobClientAvailableAtClientMap(Job job) {
		if (job.getClients() != null && job.getClients().size() > 0) { // if there is pool
			for (Client jobClient : job.getClients()) {
				if (ClientStatus.FREE.equals(clientMap.get(jobClient))) { // Is one of the jobClient available in static clientMap
					getLogger().info(job.getName() + " the job of the client " + jobClient.getName() + " client is available.");
					return true;
				}
			}
		}
		return false;
	}

	public Map<String, ClientStatus> getClientMapAsString() {
		Map<String, ClientStatus> clientMapString = new HashMap<>();
		for (Entry<Client, ClientStatus> entry : clientMap.entrySet()) {
			clientMapString.put(TedamJsonFactory.toJson(entry.getKey()), entry.getValue());
		}
		return clientMapString;
	}

	public Map<Client, ClientStatus> getClientMap() {
		return clientMap;
	}

	public void setClientMap(Map<Client, ClientStatus> clientMap) {
		this.clientMap = clientMap;
	}

}
