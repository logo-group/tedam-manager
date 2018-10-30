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

import java.io.Serializable;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.lbs.tedam.jobrunner.TedamManagerApplication;

/**
 * Web socket server for job operations.
 *
 */

@ServerEndpoint(value = "/JobManager")
public class JobRunnerSocketServer implements Serializable {

	private JobRunnerSocketServerListener listener;

	public JobRunnerSocketServer() {
		listener = TedamManagerApplication.getBeanFactory().getBean(JobRunnerSocketServerListener.class);
	}

	public JobRunnerSocketServer(JobRunnerSocketServerListener listener) {
		this.listener = listener;
	}

	/**
	 * Default serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * This method will be called when a web socket session is opened.
	 * 
	 * @param session
	 *            Session info on web socket session.
	 */
	@OnOpen
	public void onOpen(Session session) {
		listener.onOpen(session);
	}

	/**
	 * This method will be called when a web socket session is closed.
	 * 
	 * @param session
	 *            Session info on web socket session.
	 */
	@OnClose
	public void onClose(Session session) {
		listener.onClose(session);
	}

	/**
	 * This method will be called when a web socket session received a message.
	 * 
	 * @param message
	 *            String message that is received on session.
	 * 
	 * @param session
	 *            Session info on web socket session.
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		listener.onMessage(message, session);
	}

	/**
	 * This method will be called when an exception occurs.
	 * 
	 * @param error
	 *            Throwable instane.
	 */
	@OnError
	public void onError(Throwable error) {
		listener.onError(error);
	}

}
