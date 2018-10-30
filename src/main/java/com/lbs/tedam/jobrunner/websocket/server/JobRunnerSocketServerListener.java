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

package com.lbs.tedam.jobrunner.websocket.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.Session;

import com.lbs.tedam.data.service.ClientService;
import com.lbs.tedam.exception.VersionParameterValueException;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.service.BroadcastService;
import com.lbs.tedam.model.Client;
import com.lbs.tedam.model.ClientMessage;
import com.lbs.tedam.model.JobRunnerCommand;
import com.lbs.tedam.model.JobRunnerDetailCommand;
import com.lbs.tedam.model.TedamSocketMessage;
import com.lbs.tedam.util.EnumsV2.ClientStatus;
import com.lbs.tedam.util.EnumsV2.TedamSocketMessageType;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamJsonFactory;
import com.lbs.tedam.websocket.server.WebSocketServerListener;

public class JobRunnerSocketServerListener implements WebSocketServerListener, HasLogger {

	private final Map<Session, Client> sessionMap = new HashMap<>();
	private final BroadcastService broadcastService;
	private final ClientMapService clientMapService;
	private final ClientService clientService;

	public JobRunnerSocketServerListener(BroadcastService broadcastService, ClientMapService clientMapService, ClientService clientService) {
		this.broadcastService = broadcastService;
		this.clientMapService = clientMapService;
		this.clientService = clientService;
	}

	@Override
	public void onOpen(Session session) {
	}

	@Override
	public void onClose(Session session) {
		getLogger().error("onClose called");
		clientMapService.updateClientMap(sessionMap.get(session), ClientStatus.DEAD);
		removeSession(session);
	}

	@Override
	public void onMessage(String message, Session session) {
		getLogger().info("Server incoming message : " + message);
		TedamSocketMessage tedamSocketMessage = TedamJsonFactory.fromJson(message, TedamSocketMessage.class);
		if (TedamSocketMessageType.CLIENT.equals(tedamSocketMessage.getTedamSocketMessageType())) {
			ClientMessage clientMessage = TedamJsonFactory.fromJson(tedamSocketMessage.getDetail(), ClientMessage.class);
			try {
				addSession(session, clientMessage.getClientName());
				clientMapService.updateClientMap(sessionMap.get(session), clientMessage.getClientStatus());
			} catch (LocalizedException e) {
				getLogger().error(e.getLocalizedMessage(), e);
			}
		} else {
			JobRunnerDetailCommand jobRunnerDetailCommand = TedamJsonFactory.fromJson(tedamSocketMessage.getDetail(), JobRunnerDetailCommand.class);
			try {
				broadcastService.startJobCommandOperations(jobRunnerDetailCommand);
			} catch (VersionParameterValueException e) {
				getLogger().info("VersionParameterValueException : " + e);
			} catch (LocalizedException e) {
				getLogger().error(e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	public void onError(Throwable error) {
		getLogger().error("onError called");
		logException(error);
	}

	private void removeSession(Session session) {
		sessionMap.remove(session);
	}

	private void addSession(Session session, String clientName) throws LocalizedException {
		Client client = clientService.getClientByName(clientName);
		sessionMap.put(session, client);
	}

	private void logException(Throwable error) {
		getLogger().error("" + error);
	}

	public void sendJobRunnerCommand(JobRunnerCommand jobRunnerCommand) {
		Client client = jobRunnerCommand.getClient();
		String message = TedamJsonFactory.toJson(jobRunnerCommand);
		getLogger().info("Server outgoing message : " + message);
		for (Entry<Session, Client> entry : sessionMap.entrySet()) {
			if (client.equals(entry.getValue())) {
				try {
					entry.getKey().setMaxTextMessageBufferSize(123321);
					entry.getKey().getBasicRemote().sendText(message);
				} catch (Exception e) {
					getLogger().error("clientName : " + client.getName() + " message could not be sent.");
				}
			}
		}
	}

}
