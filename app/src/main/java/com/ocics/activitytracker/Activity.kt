package com.ocics.activitytracker

class Activity {
    var type: String = ""
    var startTime: String = ""

    constructor(type: String, startTime: String) {
        this.type = type
        this.startTime = startTime
    }
}