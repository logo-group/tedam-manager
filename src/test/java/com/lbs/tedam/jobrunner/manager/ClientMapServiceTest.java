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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.lbs.tedam.data.service.ClientService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.Project;
import com.lbs.tedam.util.EnumsV2;
import com.lbs.tedam.util.EnumsV2.ClientStatus;

public class ClientMapServiceTest {

	private final ClientService clientService = null;
	private ClientMapService clientMapService;
	private Job job;
	private Client client;

	@Before
	public void beforeTest() {
		clientMapService = new ClientMapService(clientService);
		client = new Client();
		job = new Job();
		clientMapService.getClientMap().put(client, EnumsV2.ClientStatus.FREE);
	}

	@Test
	public void getClientMapAsStringTest() {
		clientMapService.getClientMap().put(client, EnumsV2.ClientStatus.FREE);
		assertFalse(clientMapService.getClientMapAsString().isEmpty());
	}

	@Test
	public void isJobClientAvailableAtClientMapTestWithDeadClient() {
		job.getClients().add(client);
		clientMapService.getClientMap().put(client, EnumsV2.ClientStatus.DEAD);
		assertFalse(clientMapService.isJobClientAvailableAtClientMap(job));
	}

	@Test
	public void isJobClientAvailableAtClientMapTest() {
		job.getClients().add(client);
		clientMapService.getClientMap().put(client, EnumsV2.ClientStatus.FREE);
		assertTrue(clientMapService.isJobClientAvailableAtClientMap(job));
	}

	@Test
	public void isJobClientAvailableAtClientMapTestWithNullClientList() {
		job.setClients(null);
		boolean result = clientMapService.isJobClientAvailableAtClientMap(job);
		assertFalse(result);
	}

	@Test
	public void isJobClientAvailableAtClientMapWithEmptyClientsTest() {
		job.getClients().clear();
		boolean result = clientMapService.isJobClientAvailableAtClientMap(job);
		assertFalse(result);
	}

	@Test
	public void updateClientMapTest() {
		Client tClient = this.client;
		clientMapService.updateClientMap(tClient, EnumsV2.ClientStatus.BUSY);
		Assert.assertEquals(EnumsV2.ClientStatus.BUSY, clientMapService.getClientMap().get(tClient));
	}

	@Test
	public void updateClientMapWithNullClientTest() {
		Client tClient = null;
		clientMapService.updateClientMap(tClient, EnumsV2.ClientStatus.BUSY);
		Assert.assertNotEquals(EnumsV2.ClientStatus.BUSY, clientMapService.getClientMap().get(tClient));
	}

	@Test
	public void buildClientMapTest() throws LocalizedException {
		List<Client> clientList = new ArrayList<>();
		clientList.add(client);
		ClientService mockito = mock(ClientService.class);
		clientMapService = new ClientMapService(mockito);
		when(mockito.getClientList()).thenReturn(clientList);
		clientMapService.buildClientMap();
	}

	@Test
	public void getAvaliableClientForWithoutClientPoolTestEmptyClient() {
		clientMapService.getClientMap().clear();
		assertNull(clientMapService.getAvaliableClientForWithoutClientPool(job, true));
	}

	@Test
	public void getAvaliableClientForWithoutClientPoolTest() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.FREE);
		clientMapService.setClientMap(clientMap);
		assertNotNull(clientMapService.getAvaliableClientForWithoutClientPool(job, true));
	}

	@Test
	public void getAvaliableClientForWithFalseParameter() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.FREE);
		clientMapService.setClientMap(clientMap);
		assertNotNull(clientMapService.getAvaliableClientForWithoutClientPool(job, false));
	}

	@Test
	public void getAvaliableClientForWithDifferentObject() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(new Project());
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.FREE);
		clientMapService.setClientMap(clientMap);
		assertNull(clientMapService.getAvaliableClientForWithoutClientPool(job, false));
	}

	@Test
	public void getAvaliableClientForWithBusyClient() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.BUSY);
		clientMapService.setClientMap(clientMap);
		assertNull(clientMapService.getAvaliableClientForWithoutClientPool(job, false));
	}

	@Test
	public void getAvaliableClientTestWithEmptyClientList() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		job.getClients().clear();
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.BUSY);
		clientMapService.setClientMap(clientMap);
		assertNull(clientMapService.getAvaliableClient(job));
	}

	@Test
	public void getAvaliableClientTestWithEmptyClientMap() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		job.getClients().clear();
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMapService.setClientMap(clientMap);
		assertNull(clientMapService.getAvaliableClient(job));
	}

	@Test
	public void getAvaliableClientTestWithDifferentProject() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(new Project());
		job.getClients().clear();
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMapService.setClientMap(clientMap);
		assertNull(clientMapService.getAvaliableClient(job));
	}

	@Test
	public void getAvaliableClientTestWithNullClientList() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		job.setClients(null);
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.BUSY);
		clientMapService.setClientMap(clientMap);
		assertNull(clientMapService.getAvaliableClient(job));
	}

	@Test
	public void getAvaliableClientTest() {
		Project project = new Project();
		client.setProject(project);
		job.setProject(project);
		job.getClients().add(client);
		Map<Client, ClientStatus> clientMap = new HashMap<>();
		clientMap.put(client, EnumsV2.ClientStatus.FREE);
		clientMapService.setClientMap(clientMap);
		assertNotNull(clientMapService.getAvaliableClient(job));
	}
}
