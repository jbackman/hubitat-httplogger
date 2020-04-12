/*****************************************************************************************************************
 *  Source: https://github.com/jbackman/hubitat-httplogger

 *
 *  Raw Source: https://raw.githubusercontent.com/jbackman/hubitat-httplogger/master/hubitat-httplogger.groovy
 *
 *  Hubitat Elevation version maintained by Justin Backman
 *
 *  Description: A SmartApp to log Hubitat device states to an HTTP endpoint.
 *  Adapted from: https://github.com/HubitatCommunity/InfluxDB-Logger
 *
 *  NOTE: Hubitat does not currently support group names.
 *
 *  License:
 *   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *   for the specific language governing permissions and limitations under the License.
 *
 *   Modification History
 *   Date       Name        Change 
 *   2020-04-11 Justin Backman    Initial Commit
 *****************************************************************************************************************/
definition(
    name: "HTTP Logger",
    namespace: "jitonline",
    author: "Justin Backman (jbackman)",
    description: "Log Hubitat device states to HTTP endpoint (such as logstash)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

    import groovy.transform.Field
    import groovy.json.JsonOutput

    @Field static java.util.concurrent.ConcurrentLinkedQueue loggerQueue = new java.util.concurrent.ConcurrentLinkedQueue()
    @Field static java.util.concurrent.Semaphore mutex = new java.util.concurrent.Semaphore(1)

preferences {
          page(name: "newPage")
}


def newPage() {
    dynamicPage(name: "newPage", title: "HTTP Logger Page", install: true, uninstall: true) {
        section("General:") {
            input (
                name: "configLoggingLevelIDE",
                title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
                type: "enum",
                options: [
                    "0" : "None",
                    "1" : "Error",
                    "2" : "Warning",
                    "3" : "Info",
                    "4" : "Debug",
                    "5" : "Trace"
                ],
                defaultValue: "3",
                displayDuringSetup: true,
                required: false
            )
        }

        section ("HTTP Endpoint:") {
            input "prefHTTPScheme", "enum", title: "HTTP/HTTPS", defaultValue: "0", required: true, 
            options: ["0": 'http',"1": 'https']
            input "prefHTTPHost", "text", title: "Host", defaultValue: "192.168.1.100", required: true
            input "prefHTTPPort", "text", title: "Port", defaultValue: "8080", required: true
            input "prefHTTPPath", "text", title: "Path", defaultValue: "", required: false
            input "prefHTTPUser", "text", title: "Username", required: false
            input "prefHTTPPass", "text", title: "Password", required: false
        }
    
        section("Polling / Write frequency:") {
            input "prefSoftPollingInterval", "number", title:"Soft-Polling interval (minutes)", defaultValue: 1, required: true

            input "writeInterval", "enum", title:"How often to write to http (minutes)", defaultValue: "0", required: true,
            options: ["1",  "2", "3", "4", "5", "10", "15"]
        }
    
        section("System Monitoring:") {
            input "prefLogModeEvents", "bool", title:"Log Mode Events?", defaultValue: true, required: true
            input "prefLogHubProperties", "bool", title:"Log Hub Properties?", defaultValue: true, required: true
            input "prefLogLocationProperties", "bool", title:"Log Location Properties?", defaultValue: true, required: true
        }
    
        section("Input Format Preference:") {
            input "accessAllAttributes", "bool", title:"Get Access To All Attributes?", defaultValue: false, required: true, submitOnChange: true
        }
        
        if(!accessAllAttributes)
        {
            section("Devices To Monitor:", hideable:true,hidden:true) {
                input "accelerometers", "capability.accelerationSensor", title: "Accelerometers", multiple: true, required: false
                input "alarms", "capability.alarm", title: "Alarms", multiple: true, required: false
                input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
                input "beacons", "capability.beacon", title: "Beacons", multiple: true, required: false
                input "buttons", "capability.button", title: "Buttons", multiple: true, required: false
                input "cos", "capability.carbonMonoxideDetector", title: "Carbon Monoxide Detectors", multiple: true, required: false
                input "co2s", "capability.carbonDioxideMeasurement", title: "Carbon Dioxide Detectors", multiple: true, required: false
                input "colors", "capability.colorControl", title: "Color Controllers", multiple: true, required: false
                input "consumables", "capability.consumable", title: "Consumables", multiple: true, required: false
                input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
                input "doorsControllers", "capability.doorControl", title: "Door Controllers", multiple: true, required: false
                input "energyMeters", "capability.energyMeter", title: "Energy Meters", multiple: true, required: false
                input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity Meters", multiple: true, required: false
                input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance Meters", multiple: true, required: false
                input "locks", "capability.lock", title: "Locks", multiple: true, required: false
                input "motions", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
                input "musicPlayers", "capability.musicPlayer", title: "Music Players", multiple: true, required: false
                input "peds", "capability.stepSensor", title: "Pedometers", multiple: true, required: false
                input "phMeters", "capability.pHMeasurement", title: "pH Meters", multiple: true, required: false
                input "powerMeters", "capability.powerMeter", title: "Power Meters", multiple: true, required: false
                input "presences", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
                input "pressures", "capability.sensor", title: "Pressure Sensors", multiple: true, required: false
                input "shockSensors", "capability.shockSensor", title: "Shock Sensors", multiple: true, required: false
                input "signalStrengthMeters", "capability.signalStrength", title: "Signal Strength Meters", multiple: true, required: false
                input "sleepSensors", "capability.sleepSensor", title: "Sleep Sensors", multiple: true, required: false
                input "smokeDetectors", "capability.smokeDetector", title: "Smoke Detectors", multiple: true, required: false
                input "soundSensors", "capability.soundSensor", title: "Sound Sensors", multiple: true, required: false
                input "spls", "capability.soundPressureLevel", title: "Sound Pressure Level Sensors", multiple: true, required: false
                input "switches", "capability.switch", title: "Switches", multiple: true, required: false
                input "switchLevels", "capability.switchLevel", title: "Switch Levels", multiple: true, required: false
                input "tamperAlerts", "capability.tamperAlert", title: "Tamper Alerts", multiple: true, required: false
                input "temperatures", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
                input "thermostats", "capability.thermostat", title: "Thermostats", multiple: true, required: false
                input "threeAxis", "capability.threeAxis", title: "Three-axis (Orientation) Sensors", multiple: true, required: false
                input "touchs", "capability.touchSensor", title: "Touch Sensors", multiple: true, required: false
                input "uvs", "capability.ultravioletIndex", title: "UV Sensors", multiple: true, required: false
                input "valves", "capability.valve", title: "Valves", multiple: true, required: false
                input "volts", "capability.voltageMeasurement", title: "Voltage Meters", multiple: true, required: false
                input "waterSensors", "capability.waterSensor", title: "Water Sensors", multiple: true, required: false
                input "windowShades", "capability.windowShade", title: "Window Shades", multiple: true, required: false
            }
        } else {
            section("Devices To Monitor:", hideable:true,hidden:true) {
                input name: "allDevices", type: "capability.*", title: "Selected Devices", multiple: true, required: false, submitOnChange: true
            }

            section() {
                state.deviceList = [:]
                allDevices.each { device -> 
                    state.deviceList[device.id] = "${device.label ?: device.name}"
                }
            }
        
            section() {
                state.selectedAttr=[:]
                settings.allDevices.each { deviceName ->
                    if(deviceName) {
                        deviceId = deviceName.getId()
                        attr = deviceName.getSupportedAttributes().unique()
                        if(attr) {
                            state.options =[]
                            index = 0
                            attr.each {at->
                                state.options[index] = "${at}"
                                index = index+1
                            }
            
                            section("$deviceName", hideable: true) {
                                input name:"attrForDev$deviceId", type: "enum", title: "$deviceName", options: state.options, multiple: true, required: false, submitOnChange: true
                            }
                
                            state.selectedAttr[deviceId] = settings["attrForDev"+deviceId]
                        }
                    }
                }
            }
        }
    }
}


def getDeviceObj(id) {
    def found
    settings.allDevices.each { device -> 
        if (device.getId() == id) {
            found = device
        }
    }
    return found
}



/*****************************************************************************************************************
 *  Hubitat System Commands:
 *****************************************************************************************************************/

/**
 *  installed()
 *
 *  Runs when the app is first installed.
 **/
def installed() {
    state.installedAt = now()
    state.loggingLevelIDE = 5
    // Needs to be synchronized in case another event happens at the same time
    synchronized(this) {
        state.queuedData = []
    }
    log.debug "${app.label}: Installed with settings: ${settings}" 
}

/**
 *  uninstalled()
 *
 *  Runs when the app is uninstalled.
 **/
def uninstalled() {
    logger("uninstalled()","trace")
}

/**
 *  updated()
 * 
 *  Runs when app settings are changed.
 * 
 *  Updates device.state with input values and other hard-coded values.
 *  Builds state.deviceAttributes which describes the attributes that will be monitored for each device collection 
 *  (used by manageSubscriptions() and softPoll()).
 *  Refreshes scheduling and subscriptions.
 **/
def updated() {
    logger("updated()","trace")

    // Update internal state:
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    
    // HTTP config:
    def HTTPScheme=['http','https']
    state.HTTPScheme = HTTPScheme.get(settings.prefHTTPScheme.toInteger())
    state.HTTPHost = settings.prefHTTPHost
    state.HTTPPort = settings.prefHTTPPort
    state.HTTPPath = settings.prefHTTPPath
    state.HTTPUser = settings.prefHTTPUser
    state.HTTPPass = settings.prefHTTPPass 
    
    state.path = "/${state.HTTPPath}"
    state.headers = [:] 
    state.headers.put("HOST", "${state.HTTPHost}:${state.HTTPPort}")
    if (state.HTTPUser && state.HTTPPass) {
        state.headers.put("Authorization", encodeCredentialsBasic(state.HTTPUser, state.HTTPPass))
    }

    // Build array of device collections and the attributes we want to report on for that collection:
    //  Note, the collection names are stored as strings. Adding references to the actual collection 
    //  objects causes major issues (possibly memory issues?).
    state.deviceAttributes = []
    state.deviceAttributes << [ devices: 'accelerometers', attributes: ['acceleration']]
    state.deviceAttributes << [ devices: 'alarms', attributes: ['alarm']]
    state.deviceAttributes << [ devices: 'batteries', attributes: ['battery']]
    state.deviceAttributes << [ devices: 'beacons', attributes: ['presence']]
    state.deviceAttributes << [ devices: 'buttons', attributes: ['button']]
    state.deviceAttributes << [ devices: 'cos', attributes: ['carbonMonoxide']]
    state.deviceAttributes << [ devices: 'co2s', attributes: ['carbonDioxide']]
    state.deviceAttributes << [ devices: 'colors', attributes: ['hue','saturation','color']]
    state.deviceAttributes << [ devices: 'consumables', attributes: ['consumableStatus']]
    state.deviceAttributes << [ devices: 'contacts', attributes: ['contact']]
    state.deviceAttributes << [ devices: 'doorsControllers', attributes: ['door']]
    state.deviceAttributes << [ devices: 'energyMeters', attributes: ['energy']]
    state.deviceAttributes << [ devices: 'humidities', attributes: ['humidity']]
    state.deviceAttributes << [ devices: 'illuminances', attributes: ['illuminance']]
    state.deviceAttributes << [ devices: 'locks', attributes: ['lock']]
    state.deviceAttributes << [ devices: 'motions', attributes: ['motion']]
    state.deviceAttributes << [ devices: 'musicPlayers', attributes: ['status','level','trackDescription','trackData','mute']]
    state.deviceAttributes << [ devices: 'peds', attributes: ['steps','goal']]
    state.deviceAttributes << [ devices: 'phMeters', attributes: ['pH']]
    state.deviceAttributes << [ devices: 'powerMeters', attributes: ['power','voltage','current','powerFactor']]
    state.deviceAttributes << [ devices: 'presences', attributes: ['presence']]
    state.deviceAttributes << [ devices: 'pressures', attributes: ['pressure']]
    state.deviceAttributes << [ devices: 'shockSensors', attributes: ['shock']]
    state.deviceAttributes << [ devices: 'signalStrengthMeters', attributes: ['lqi','rssi']]
    state.deviceAttributes << [ devices: 'sleepSensors', attributes: ['sleeping']]
    state.deviceAttributes << [ devices: 'smokeDetectors', attributes: ['smoke']]
    state.deviceAttributes << [ devices: 'soundSensors', attributes: ['sound']]
    state.deviceAttributes << [ devices: 'spls', attributes: ['soundPressureLevel']]
    state.deviceAttributes << [ devices: 'switches', attributes: ['switch']]
    state.deviceAttributes << [ devices: 'switchLevels', attributes: ['level']]
    state.deviceAttributes << [ devices: 'tamperAlerts', attributes: ['tamper']]
    state.deviceAttributes << [ devices: 'temperatures', attributes: ['temperature']]
    state.deviceAttributes << [ devices: 'thermostats', attributes: ['temperature','heatingSetpoint','coolingSetpoint','thermostatSetpoint','thermostatMode','thermostatFanMode','thermostatOperatingState','thermostatSetpointMode','scheduledSetpoint','optimisation','windowFunction']]
    state.deviceAttributes << [ devices: 'threeAxis', attributes: ['threeAxis']]
    state.deviceAttributes << [ devices: 'touchs', attributes: ['touch']]
    state.deviceAttributes << [ devices: 'uvs', attributes: ['ultravioletIndex']]
    state.deviceAttributes << [ devices: 'valves', attributes: ['contact']]
    state.deviceAttributes << [ devices: 'volts', attributes: ['voltage']]
    state.deviceAttributes << [ devices: 'waterSensors', attributes: ['water']]
    state.deviceAttributes << [ devices: 'windowShades', attributes: ['windowShade']]

    // Configure Scheduling:
    state.softPollingInterval = settings.prefSoftPollingInterval.toInteger()
    state.writeInterval = settings.writeInterval
    manageSchedules()
    
    // Configure Subscriptions:
    manageSubscriptions()
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/

/**
 *  handleAppTouch(evt)
 * 
 *  Used for testing.
 **/
def handleAppTouch(evt) {
    logger("handleAppTouch()","trace")
    
    softPoll()
}

/**
 *  handleModeEvent(evt)
 * 
 *  Log Mode changes.
 **/
def handleModeEvent(evt) {
    logger("handleModeEvent(): Mode changed to: ${evt.value}","info")

    def locationId = location.id.toString()
    def locationName = location.name
    def mode = evt.value
    def data = [measurement: "_stMode", locationId: "${locationId}", locationName: "${locationName}", mode: "${mode}"]
    queueToHTTP(data)
}

/**
 *  handleEvent(evt)
 *
 *  Builds data to send to HTTP.
 *   - Escapes and quotes string values.
 *   - Calculates logical binary values where string values can be 
 *     represented as binary values (e.g. contact: closed = 1, open = 0)
 * 
 *  Useful references: 
 *   - http://docs.smartthings.com/en/latest/capabilities-reference.html
 **/
def handleEvent(evt) {
    //logger("handleEvent(): $evt.unit","info")
    logger("handleEvent(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
    
    // Build data string to send to HTTP:
    //  Format: <measurement>[,<tag_name>=<tag_value>] field=<field_value>
    //    If value is an integer, it must have a trailing "i"
    //    If value is a string, it must be enclosed in double quotes.
    def measurement = evt.name
    // tags:
    def deviceId = evt.deviceId.toString()
    def deviceName = evt.displayName
    def groupId = evt?.device.device.groupId
    def groupName = getGroupName(evt?.device.device.groupId)
    def hubId = evt?.device.device.hubId.toString()
    def hubName = evt?.device.device.hub.name.toString()
    // Don't pull these from the evt.device as the app itself will be associated with one location.
    def locationId = location.id.toString()
    def locationName = location.name

    def unit = evt.unit
    def value = evt.value
    def valueBinary = ''
    
    def data = [measurement: "${measurement}", deviceId: "${deviceId}", deviceName: "${deviceName}", groupId: "${groupId}", groupName: "${groupName}", hubId: "${hubId}", hubName: "${hubName}", locationId: "${locationId}", locationName: "${locationName}"]
    
    // Unit tag and fields depend on the event type:
    //  Most string-valued attributes can be translated to a binary value too.
    if ('acceleration' == evt.name) { // acceleration: Calculate a binary value (active = 1, inactive = 0)
        unit = 'acceleration'
        value = '"' + value + '"'
        valueBinary = ('active' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('alarm' == evt.name) { // alarm: Calculate a binary value (strobe/siren/both = 1, off = 0)
        unit = 'alarm'
        value = '"' + value + '"'
        valueBinary = ('off' == evt.value) ? '0i' : '1i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('button' == evt.name) { // button: Calculate a binary value (held = 1, pushed = 0)
        unit = 'button'
        value = '"' + value + '"'
        valueBinary = ('pushed' == evt.value) ? '0i' : '1i'
        data << [unit: "${unit}", value: "${value}",valueBinary: "${valueBinary}"]
    }
    else if ('carbonMonoxide' == evt.name) { // carbonMonoxide: Calculate a binary value (detected = 1, clear/tested = 0)
        unit = 'carbonMonoxide'
        value = '"' + value + '"'
        valueBinary = ('detected' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('consumableStatus' == evt.name) { // consumableStatus: Calculate a binary value ("good" = 1, "missing"/"replace"/"maintenance_required"/"order" = 0)
        unit = 'consumableStatus'
        value = '"' + value + '"'
        valueBinary = ('good' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('contact' == evt.name) { // contact: Calculate a binary value (closed = 1, open = 0)
        unit = 'contact'
        value = '"' + value + '"'
        valueBinary = ('closed' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('door' == evt.name) { // door: Calculate a binary value (closed = 1, open/opening/closing/unknown = 0)
        unit = 'door'
        value = '"' + value + '"'
        valueBinary = ('closed' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('lock' == evt.name) { // door: Calculate a binary value (locked = 1, unlocked = 0)
        unit = 'lock'
        value = '"' + value + '"'
        valueBinary = ('locked' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('motion' == evt.name) { // Motion: Calculate a binary value (active = 1, inactive = 0)
        unit = 'motion'
        value = '"' + value + '"'
        valueBinary = ('active' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('mute' == evt.name) { // mute: Calculate a binary value (muted = 1, unmuted = 0)
        unit = 'mute'
        value = '"' + value + '"'
        valueBinary = ('muted' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('presence' == evt.name) { // presence: Calculate a binary value (present = 1, not present = 0)
        unit = 'presence'
        value = '"' + value + '"'
        valueBinary = ('present' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('shock' == evt.name) { // shock: Calculate a binary value (detected = 1, clear = 0)
        unit = 'shock'
        value = '"' + value + '"'
        valueBinary = ('detected' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('sleeping' == evt.name) { // sleeping: Calculate a binary value (sleeping = 1, not sleeping = 0)
        unit = 'sleeping'
        value = '"' + value + '"'
        valueBinary = ('sleeping' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('smoke' == evt.name) { // smoke: Calculate a binary value (detected = 1, clear/tested = 0)
        unit = 'smoke'
        value = '"' + value + '"'
        valueBinary = ('detected' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('sound' == evt.name) { // sound: Calculate a binary value (detected = 1, not detected = 0)
        unit = 'sound'
        value = '"' + value + '"'
        valueBinary = ('detected' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('switch' == evt.name) { // switch: Calculate a binary value (on = 1, off = 0)
        unit = 'switch'
        value = '"' + value + '"'
        valueBinary = ('on' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('tamper' == evt.name) { // tamper: Calculate a binary value (detected = 1, clear = 0)
        unit = 'tamper'
        value = '"' + value + '"'
        valueBinary = ('detected' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('thermostatMode' == evt.name) { // thermostatMode: Calculate a binary value (<any other value> = 1, off = 0)
        unit = 'thermostatMode'
        value = '"' + value + '"'
        valueBinary = ('off' == evt.value) ? '0i' : '1i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('thermostatFanMode' == evt.name) { // thermostatFanMode: Calculate a binary value (<any other value> = 1, off = 0)
        unit = 'thermostatFanMode'
        value = '"' + value + '"'
        valueBinary = ('off' == evt.value) ? '0i' : '1i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('thermostatOperatingState' == evt.name) { // thermostatOperatingState: Calculate a binary value (heating = 1, <any other value> = 0)
        unit = 'thermostatOperatingState'
        value = '"' + value + '"'
        valueBinary = ('heating' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('thermostatSetpointMode' == evt.name) { // thermostatSetpointMode: Calculate a binary value (followSchedule = 0, <any other value> = 1)
        unit = 'thermostatSetpointMode'
        value = '"' + value + '"'
        valueBinary = ('followSchedule' == evt.value) ? '0i' : '1i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('threeAxis' == evt.name) { // threeAxis: Format to x,y,z values.
        unit = 'threeAxis'
        def valueXYZ = evt.value.split(",")
        def valueX = valueXYZ[0]
        def valueY = valueXYZ[1]
        def valueZ = valueXYZ[2]
        data << [unit: "${unit}", valueX: "${valueX}", valueY: "${valueY}", valueZ: "${valueZ}"] // values are integers.
    }
    else if ('touch' == evt.name) { // touch: Calculate a binary value (touched = 1, "" = 0)
        unit = 'touch'
        value = '"' + value + '"'
        valueBinary = ('touched' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('optimisation' == evt.name) { // optimisation: Calculate a binary value (active = 1, inactive = 0)
        unit = 'optimisation'
        value = '"' + value + '"'
        valueBinary = ('active' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('windowFunction' == evt.name) { // windowFunction: Calculate a binary value (active = 1, inactive = 0)
        unit = 'windowFunction'
        value = '"' + value + '"'
        valueBinary = ('active' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('touch' == evt.name) { // touch: Calculate a binary value (touched = 1, <any other value> = 0)
        unit = 'touch'
        value = '"' + value + '"'
        valueBinary = ('touched' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('water' == evt.name) { // water: Calculate a binary value (wet = 1, dry = 0)
        unit = 'water'
        value = '"' + value + '"'
        valueBinary = ('wet' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    else if ('windowShade' == evt.name) { // windowShade: Calculate a binary value (closed = 1, <any other value> = 0)
        unit = 'windowShade'
        value = '"' + value + '"'
        valueBinary = ('closed' == evt.value) ? '1i' : '0i'
        data << [unit: "${unit}", value: "${value}", valueBinary: "${valueBinary}"]
    }
    // Catch any other event with a string value that hasn't been handled:
    else if (evt.value ==~ /.*[^0-9\.,-].*/) { // match if any characters are not digits, period, comma, or hyphen.
        logger("handleEvent(): Found a string value that's not explicitly handled: Device Name: ${deviceName}, Event Name: ${evt.name}, Value: ${evt.value}","warn")
        value = '"' + value + '"'
        data << [unit: "${unit}", value: "${value}"]
    }
    // Catch any other general numerical event (carbonDioxide, power, energy, humidity, level, temperature, ultravioletIndex, voltage, etc).
    else {
        data << [unit: "${unit}", value: "${value}"]
    }
    
    logger("$data","info")
    
    // Queue data for later write to HTTP
    queueToHTTP(data)
}


/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/

/**
 *  softPoll()
 *
 *  Executed by schedule.
 * 
 *  Forces data to be posted to HTTP (even if an event has not been triggered).
 *  Doesn't poll devices, just builds a fake event to pass to handleEvent().
 *
 *  Also calls LogSystemProperties().
 **/
def softPoll() {
    logger("softPoll()","trace")
    
    logSystemProperties()
    if(!accessAllAttributes) {
        // Iterate over each attribute for each device, in each device collection in deviceAttributes:
        def devs // temp variable to hold device collection.
        state.deviceAttributes.each { da ->
            devs = settings."${da.devices}"
            if (devs && (da.attributes)) {
                devs.each { d ->
                    da.attributes.each { attr ->
                        if (d.hasAttribute(attr) && d.latestState(attr)?.value != null) {
                            logger("softPoll(): Softpolling device ${d} for attribute: ${attr}","info")
                            // Send fake event to handleEvent():

                            handleEvent([
                                name: attr, 
                                value: d.latestState(attr)?.value,
                                unit: d.latestState(attr)?.unit,
                                device: d,
                                deviceId: d.id,
                                displayName: d.displayName
                            ])
                        }
                    }
                }
            }
        }
    } else {
        state.selectedAttr.each{ entry -> 
            d = getDeviceObj(entry.key)
            entry.value.each{ attr ->
                if (d.hasAttribute(attr) && d.latestState(attr)?.value != null) {
                    logger("softPoll(): Softpolling device ${d} for attribute: ${attr}","info")
                    // Send fake event to handleEvent():
                    handleEvent([
                        name: attr, 
                        value: d.latestState(attr)?.value,
                        unit: d.latestState(attr)?.unit,
                        device: d,
                        deviceId: d.id,
                        displayName: d.displayName
                    ])
                }
            }
        }
    }
}

/**
 *  logSystemProperties()
 *
 *  Generates measurements for Hubitat system (hubs and locations) properties.
 **/
def logSystemProperties() {
    logger("logSystemProperties()","trace")

    def locationId = location.id.toString()
    def locationName = location.name

    // Location Properties:
    if (prefLogLocationProperties) {
        try {
            def tz = location.timeZone.ID.toString()
            def mode = location.mode
            def hubCount = location.hubs.size()
            def times = getSunriseAndSunset()
            def srt = '"' + times.sunrise.format("HH:mm", location.timeZone) + '"'
            def sst = '"' + times.sunset.format("HH:mm", location.timeZone) + '"'

            def data = [measurement: "_heLocation", locationId: "${locationId}", locationName: "${locationName}", latitude: "${location.latitude}",longitude: "${location.longitude}",timeZone: "${tz}", mode: "${mode}", hubCount: "${hubCount}", sunriseTime: "${srt}", sunsetTime: "${sst}"]
            queueToHTTP(data)
        } catch (e) {
            logger("logSystemProperties(): Unable to log Location properties: ${e}","error")
        }
    }

    // Hub Properties:
    if (prefLogHubProperties) {
           location.hubs.each { h ->
            try {
                def hubId = h.id.toString()
                def hubName = h.name.toString()
                def hubIP = h.localIP.toString()
                def firmwareVersion =  h.firmwareVersionString
                
                def data = [measurement: "_heHub", locationId: "${locationId}", locationName: "${locationName}", hubId: "${hubId}", hubName: "${hubName}", hubIP: "${hubIP}"]
                data << [firmwareVersion: "${firmwareVersion}"]
                queueToHTTP(data)
            } catch (e) {
                logger("logSystemProperties(): Unable to log Hub properties: ${e}","error")
            }
           }

    }
}

def queueToHTTP(data) {
    // Add timestamp 
    long timeNow = (new Date().time) * 1e6 // Time is in milliseconds, needs to be in nanoseconds
    data << [timestamp: "${timeNow}"]
    
    int queueSize = 0
    try {
        mutex.acquire()
        
        loggerQueue.offer(data)
        queueSize = loggerQueue.size()
        
        // Give some visibility at the interface level
        state.queuedData = loggerQueue.toArray()
    } 
    catch(e) {
        logger("Error 2 in queueToHTTP","Warning")
    } 
    finally {
        mutex.release()
    }
    
    if (queueSize > 100) {
        logger("Queue size is too big, triggering write now", "info")
        writeQueuedDataToHTTP()
    }
}

def writeQueuedDataToHTTP() {
    String writeData = ""
    
    try {
        mutex.acquire()
                
        if(loggerQueue.size() == 0) {
            logger("No queued data to write to HTTP", "info")
            return
        }
        logger("Writing queued data of size ${loggerQueue.size()} out", "info")
        a = loggerQueue.toArray() 
        writeData = JsonOutput.toJson(a)
        loggerQueue.clear()
        state.queuedData = []
    }       
    catch(e) {
        logger("Error 2 in writeQueuedDataToHTTP","Warning")
    } 
    finally {
        mutex.release()
    }
    
    postToHTTP(writeData)
}

/**
 *  postToHTTP()
 *
 *  Posts data to HTTP.
 *
 **/
def postToHTTP(data) {
    logger("postToHTTP(): Posting data to HTTP: Host: ${state.HTTPHost}, Port: ${state.HTTPPort}, HTTP: ${state.HTTPPath}","info")

    // Hubitat Async http Post
     
    try {
        def postParams = [
            uri: "${state.HTTPScheme}://${state.HTTPHost}:${state.HTTPPort}/${state.HTTPPath}" ,
            requestContentType: 'application/json',
            contentType: 'application/json',
            body : data
            ]
        asynchttpPost('handleHTTPResponse', postParams) 
    } catch (e) {    
        logger("postToHTTP(): Something went wrong when posting: ${e}","error")
    }

}

/**
 *  handleHTTPResponse()
 *
 *  Handles response from post made in postToHTTP().
 **/
def handleHTTPResponse(hubResponse, data) {
    if(hubResponse.status >= 400) {
        logger("postToHTTP(): Something went wrong! Response from HTTP: Status: ${hubResponse.status}, Headers: ${hubResponse.headers}, Data: ${data}","error")
    }
}


/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

/**
 *  manageSchedules()
 * 
 *  Configures/restarts scheduled tasks: 
 *   softPoll() - Run every {state.softPollingInterval} minutes.
 **/
private manageSchedules() {
    logger("manageSchedules()","trace")

    // Generate a random offset (1-60):
    Random rand = new Random(now())
    def randomOffset = 0
    
    try {
        unschedule(softPoll)
        unschedule(writeQueuedDataToHTTP)
    }
    catch(e) {
        // logger("manageSchedules(): Unschedule failed!","error")
    }

    randomOffset = rand.nextInt(50)
    if (state.softPollingInterval > 0) {
        
        logger("manageSchedules(): Scheduling softpoll to run every ${state.softPollingInterval} minutes (offset of ${randomOffset} seconds).","trace")
        schedule("${randomOffset} 0/${state.softPollingInterval} * * * ?", "softPoll")
    }
    
    randomOffset = randomOffset+8
    schedule("${randomOffset} 0/${state.writeInterval} * * * ?", "writeQueuedDataToHTTP")
}

/**
 *  manageSubscriptions()
 * 
 *  Configures subscriptions.
 **/
private manageSubscriptions() {
    logger("manageSubscriptions()","trace")

    // Unsubscribe:
    unsubscribe()
    
    // Subscribe to App Touch events:
    subscribe(app,handleAppTouch)
    
    // Subscribe to mode events:
    if (prefLogModeEvents) subscribe(location, "mode", handleModeEvent)
    
    if(!accessAllAttributes) {
        // Subscribe to device attributes (iterate over each attribute for each device collection in state.deviceAttributes):
        def devs // dynamic variable holding device collection.
        state.deviceAttributes.each { da ->
            devs = settings."${da.devices}"
            if (devs && (da.attributes)) {
                da.attributes.each { attr ->
                    logger("manageSubscriptions(): Subscribing to attribute: ${attr}, for devices: ${da.devices}","info")
                    // There is no need to check if all devices in the collection have the attribute.
                    subscribe(devs, attr, handleEvent)
                }
            }
        }
    } else {
        state.selectedAttr.each{ entry -> 
            d = getDeviceObj(entry.key)
            entry.value.each{ attr ->
                logger("manageSubscriptions(): Subscribing to attribute: ${attr}, for device: ${d}","info")
                subscribe(d, attr, handleEvent)
            }
        }
    }
}

/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

/**
 *  encodeCredentialsBasic()
 *
 *  Encode credentials for HTTP Basic authentication.
 **/
private encodeCredentialsBasic(username, password) {
    def rawString = "Basic " + "${username}:${password}"
    return rawString.bytes.encodeBase64().toString()
}

/**
 *  getGroupName()
 *
 *  Get the name of a 'Group' (i.e. Room) from its ID.
 *  
 *  This is done manually as there does not appear to be a way to enumerate
 *  groups from a SmartApp currently.
 * 
 *  GroupIds can be obtained from the Hubitat IDE under 'My Locations'.
 *
 **/
private getGroupName(id) {

    if (id == null) {return 'Home'}
    //else if (id == 'XXXXXXXXXXXXX') {return 'Group'}
    else {return 'Unknown'}    
}
