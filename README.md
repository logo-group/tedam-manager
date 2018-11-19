# TEDAM-Manager
<a href="http://www.logo.com.tr"><img src="https://www.logo.com.tr/img/logo.png"/></a>

[![Build Status](https://travis-ci.com/logobs/tedam-manager.svg?branch=master)](https://travis-ci.com/logobs/tedam-manager)
[![sonar-quality-gate][sonar-quality-gate]][sonar-url] [![sonar-bugs][sonar-bugs]][sonar-url] [![sonar-vulnerabilities][sonar-vulnerabilities]][sonar-url] [![sonar-duplicated-lines][sonar-dublicated-lines]][sonar-url]

[sonar-url]: https://sonarcloud.io/dashboard?id=com.lbs.tedam%3ATedamManager
[sonar-quality-gate]: https://sonarcloud.io/api/project_badges/measure?project=com.lbs.tedam%3ATedamManager&metric=alert_status
[sonar-bugs]: https://sonarcloud.io/api/project_badges/measure?project=com.lbs.tedam%3ATedamManager&metric=bugs
[sonar-vulnerabilities]: https://sonarcloud.io/api/project_badges/measure?project=com.lbs.tedam%3ATedamManager&metric=vulnerabilities
[sonar-dublicated-lines]: https://sonarcloud.io/api/project_badges/measure?project=com.lbs.tedam%3ATedamManager&metric=duplicated_lines_density


Tedam Manager is an essential part of the TEDAM ecosystem. Tedam Manager can work itself like TedamFace and Tedam Agent.<br>
In general, it is the module that provides the coordination of the system. Basic responsibilities of Tedam Manager are;<br>
•	Transform test scenarios that entered via Tedam Face to executable commands,<br>
•	Send executable commands to available Tedam Agents,<br>
•	Collect test run results from Tedam Agent,<br>
•	Work as REST API interface,<br>
•	Notify user or 3rd party software with test results.<br>

Tedam Manager contains the following parts:<br>

-WebSocket<br>
-Rest API<br>
-Notifier<br>
-JobRunnerEngine<br>
-ClientPool<br>

WebSocket: It is the communication component between Tedam Manager and Tedam Agent. There is a real time messaging mechanism for connection/disconnection operations. Connection status of the Tedam Agent reflected to ClientPool instantly. So that the list of agents to be selected for running jobs is always up to date.<br>

The data to be sent to the Tedam Agent and the information generated after the test run are processed through the websocket.<br>

RestAPI: Endpoint that is opened for both Tedam Face and Tedam Agent. The necessary data getter and setter operations are performed.<br>
Notifier: Component that is used for to notify user or 3rd party software about the job execution results. Generic structure of the Notifier enables user to implement any kind of notification format (mail, slack, twit etc.)<br>

JobRunnerEngine: It is generated for each SUT (System Under Test) managed by TEDAM. JobRunnerEngine is responsible for queuing and running of job and collecting the results.<br>

ClientPool: It is the component in which the TEDAM Agents status is updated in real time through websocket. Client selection is handled by ClientPool during job execution.
