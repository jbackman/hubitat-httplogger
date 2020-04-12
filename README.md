# HTTP Logger

Copyright (c) [Justin Backman](https://github.com/jbackman)

## Overview

This Hubitat App logs Hubitat device attributes to a HTTP endpoint.

This is unabashidly adapted from work done by:

[David Lomas](https://github.com/codersaur): [SmartThings/smartapps/influxdb-logger](https://github.com/codersaur/SmartThings/blob/master/smartapps/influxdb-logger/)

[Joshua Marker](https://github.com/tooluser): [HubitatCommunity/InfluxDB-Logger](https://github.com/HubitatCommunity/InfluxDB-Logger)

### Key features:
* Changes to device attributes are immediately logged to HTTP endpoint.
* The _Soft-Polling_ feature forces attribute values to be written to the HTTP endpoint periodically, even if values haven't changed.
* Logs Location _Mode_ events.
* Supports Basic Authentication to a HTTP endpoint.

## Installation
1. In the Hubitat main screen, click on the "Apps Code" link 
2. Click on the "New App" button in the top right corner
3. Click on the "Import" button
4. Paste the following link into the text box: https://raw.githubusercontent.com/jbackman/hubitat-httplogger/master/http-logger.groovy
5. Click on the "Import" button
6. Click on the "Save" button

## Activation
1. The app must be installed per above
2. In the Hubitat main screen, click on the "Apps" link
3. Click on "Add User App"
4. Click on the "HTTP Logger"
5. Configure the settings per below

### Usage
App settings:
* **IDE Live Logging Level**: Messages with this level and higher will be logged to the IDE.
* **HTTP Endpoint:**: Specify your HTTP endpoint details in this section.
* **Polling**: Configure the _Soft-Polling_ interval. All device attribute values will be written to the database at least once per interval. This is useful to ensure attribute values are written to the database, even when they have not changed. Set to zero to disable.
* **System Monitoring**: Configure which location and hub attributes are logged.
* **Devices to Monitor**: Specify which device attributes to monitor.

## Version History

#### 2020-04-11: v1.0
Initial commit
