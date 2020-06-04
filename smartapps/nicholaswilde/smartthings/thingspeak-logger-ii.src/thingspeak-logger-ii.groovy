definition(
    name: "ThingSpeak Logger II",
    namespace: "nicholaswilde/smartthings",
    author: "Nicholas Wilde",
    description: "Log events to ThingSpeak",
    category: "Convenience",
    iconUrl: "https://lh6.googleusercontent.com/-LCt2nG3Npbo/AAAAAAAAAAI/AAAAAAAAAB8/O6sWrun4q6w/s50-c/photo.jpg",
    iconX2Url: "https://lh6.googleusercontent.com/-LCt2nG3Npbo/AAAAAAAAAAI/AAAAAAAAAB8/O6sWrun4q6w/s100-c/photo.jpg")

preferences {
    section("Log devices...") {
    	input "illum", "capability.illuminanceMeasurement", title: "Illuminance", required:false, multiple: true
        input "hum", "capability.relativeHumidityMeasurement", title: "Humidity", required:false, multiple: true
        input "temp", "capability.temperatureMeasurement", title: "Temperature", required:false, multiple: true
        input "uv", "capability.ultravioletIndex", title: "UV", required: false, multiple: true
        input "accel", "capability.accelerationSensor", title: "Acceleration", required: false, multiple: true
        input "movement", "capability.motionSensor", title: "Motion", required: false, multiple: true
        input "tampering", "capability.tamperAlert", title: "Tamper", required: false, multiple: true
        input "batt", "capability.battery", title: "Battery", required: false, multiple: true
        input "barometer", "capability.airpressure", title: "Barometer", required: false, multiple: true
        input "spl", "capability.soundlevel", title: "SPL Meter", required: false, multiple: true
    }

    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel ID"
    }

    section ("ThinkSpeak write key...") {
        input "channelKey", "password", title: "Channel Key"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    //  (input evt, 'thingspeakfieldname', handlerfunc)
    subscribe(illum, "illuminance", handleStringEvent)
    subscribe(hum, "humidity", handleStringEvent)
    subscribe(temp, "temperature", handleStringEvent)
    subscribe(uv, "ultraviolet", handleStringEvent)
    subscribe(batt, "battery", handleStringEvent)
    subscribe(barometer, "airpressure", handleStringEvent)
    subscribe(spl, "soundlevel", handleStringEvent)
    subscribe(accel, "acceleration", handleAccelerationEvent)
    subscribe(movement, "motion", handleMotionEvent)
    subscribe(tampering, "tamper", handleTamperEvent)

    updateChannelInfo()
    log.debug state.fieldMap
}

def handleStringEvent(evt) {
    logField(evt) { it.toString() }
}

def handleAccelerationEvent(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}

def handleMotionEvent(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}

def handleTamperEvent(evt) {
    logField(evt) { it == "detected" ? "1" : "0" }
}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim().toLowerCase()] = it.key }
    return fieldMap
}

private updateChannelInfo() {
    log.debug "Retrieving channel info for ${channelId}"

    def url = "http://api.thingspeak.com/channels/${channelId}/feed.json?key=${channelKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak data retrieval failed, status = ${response.status}"
        } else {
        	log.debug "ThingSpeak data retrieval successful, status = ${response.status}"
            state.channelInfo = response.data?.channel
        }
    }

    state.fieldMap = getFieldMap(state.channelInfo)
}

private logField(evt, Closure c) {
    def deviceName = evt.name.trim().toLowerCase()
    log.debug state.fieldMap
    def fieldNum = state.fieldMap[deviceName]
    if (!fieldNum) {
    log.debug "Device '${deviceName}' has no field"
        return
    }

    def value = c(evt.value)
    log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"

    def url = "http://api.thingspeak.com/update?key=${channelKey}&${fieldNum}=${value}"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        } else {
        	log.debug "ThingSpeak logging successful, status = ${response.status}"
        }
    }
}