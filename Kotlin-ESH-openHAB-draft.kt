// Kotlin Rule, simplest version
// texts after // are comments
// this syntax is not exaustive, many examples and documentation coming soon
rule "Michael Wakesup" {
    triggerWhen { "Bedroom1.Lamp".is(ON) && currentTime.isAfter(SUNRISE + 30.minutes) }
    dontRetriggerWithin { 23.hours }
    actions {
        // To specify a target you may use any of: 
        //      Thing Label, ThingUID, ItemName, ChannelUID 
        //      Thing Label.channelName
        //      Location.Thing Label
        //      Location.Thing Label.channelName
        // system figures out what you mean. This intelligent dispatcher is already implemented
        // see below how dispatch works
        //sendCommand sends outgoing command
        //updateState only updates state at server
        
        // Bedroom1_Light is an item name
        "Bedroom1_Light".sendCommand(ON) // Kotin extension function style
        
        // Bedroom1 is Thing location and Lamp is Thing label
        // sendCommand synonym is kept for backwards compatibility to rules DSL
        sendCommand("Bedroom1.Lamp", OFF) // traditional function style
        
        // Internet Radio1 below is thing label
        // If multiple ambiguos things/channels are found, this won't do anything, 
        //      in such a case either change Thing Labels or use ItemName or ChannelUID
        // command goes to Internet Radio1's power channel if:
        //      power is only channel that accepts OnOffType OR 
        //      power is tagged as default/catchall channel for that ThingType
        "Internet Radio1".sendCommand(ON)
        
        // channel volume inferred automatically based on data type
        "Internet Radio1".sendCommand(60.percent) 
        
        // explicit channel specification
        "Internet Radio1.station".sendCommand("AltRock2") 
        
        // update family member state
        "Our.Michael".updateState(AWAKE)
        // other states: AWAY, ASLEEP, OUTSTATION, ATSCHOOL, ATWORK, any suggestions?
        // To create family, you have to create String Items and tag them with "family"
        // other tags could be "friends", "relatives", "pets", "guests", any suggestions?
    }
}

// Kotlin Rule, advanced version, could be in same or different file
rule "My Kotlin Rule1" {
    //optional. 
    // this weekly scheduling engine is implemented already
    // TODO: monthly and yearly schedule support
    enabledAt { 
        days(MONDAY..FRIDAY) {
            during(1530.mt..MIDNIGHT) // mt means military time, 00:00 to 23:59
            during(MIDNIGHT..SUNRISE)
        }
        day(SATURDAY)
    }
    // there could be multiple enabledAt clauses
    
    //optional
    forbiddenAt { 
        day(SUNDAY) {
            during(630.mt..NOON)
            during(1530.mt..SUNSET)
        }
        day(WEDNESDAY)
    }
    // there could be multiple forbiddenAt clauses
    // forbiddenAt takes priority over enabledAt
    
    // if current time is outside enabledAt and forbiddenAt, should the rule be enabled?
    enabledByDefault { false }
    
    // optional. how long before rule is allowed to execute again.
    // default 3.seconds
    dontRetriggerWithin { 30.minutes }
    
    // periodically trigger rule
    // honors dontRetriggerWithin, forbiddenAt and suppressWhen conditions
    // uncommented here since doesn't make sense for this demo use case of intrusion detection
    // retriggerEvery { 2.hours }
    // retriggerEvery { SUNDAY.at(NOON) }
    // retriggerEvery { SUNDAY.at(1530.mt) } // mt means military time, 00:00 to 23:59 hours
    // retriggerEvery { SUNRISE+30.minutes }
    // there could be multiple  retriggerEvery clauses
    
    //optional aliases for site specific mappings and readability
    aliases {
        "Light1".aliasToItem("very_very_long_item_name1")
        "Light2".aliasToItem("very_very_long_item_name2")
        "Door1".aliasToChannel("very:very:long:channel:uid1")
        "Motion1".aliasToChannel("very:very:long:channel:uid2")
        "MotionSensor1".aliasToThing("very:very:long:thing:uid1")
        "FrontMotion".aliasToThing("very:very:long:thing:uid1")
        // multiple aliases can point to same target device
    }
    // there could be multiple aliases clauses
    
    // optional
    triggerWhen { "MotionSensor1".is(OFFLINE) && !"Light1".is(ON) &&
        "Door1".goesFrom(CLOSED, OPEN) 
    }
    //there could be multiple triggerWhen clauses. rule triggers if at least one clause is satisfied
    // other usage: 
    // Outdoor is location, Light is thing lable below
    // ==========================================================
    //  "Outdoor.Light".receivedCommand(ON)
    //  "Outdoor.Light.power".receivedUpdate(OFF)
    //  "Outdoor.Light".is(OFF)
    //  "Outdoor.Light".goes(OFF)
    //  "Outdoor.Light".goesFrom(ON, OFF)
    //  "Outdoor.Light".is(OFFLINE)
    //  "Outdoor.Light.power".goes(OFFLINE)
    //  "Outdoor.Light".goesFrom(ONLINE, OFFLINE)
    // ==========================================================
    // is(), goes() and goesFrom() can operate on item/thing/channel
    // if you use thing status like ONLINE/OFFLINE, the target thing is inferred automatically
    
    // optional suppressWhen clauses to not trigger rule when certain conditions are met.
    // they take priority over trigger-when clauses
    // rule does not trigger if at least one clause is satisfied
    suppressWhen { "Door1".is(CLOSED) && "Motion1".is(ONLINE) && "Motion1".is(OPEN) }
    
    // optional
    // continue when thing, item, channel not found or not ready. similar to bash's set -e
    continueOnErrors { true }
    
    actions {
        // actions go here. free form Kotlin script, with IDE autocomplete,
        // and some nice helpers available in the context
        val msg = "Intrusion alert, suspicious activity near Door1"
        // use a predefined function from standard ESH Kotlin extension
        // openHAB actions are also supported
        sendUiNotification(msg)

        // actions on items channels things
        "Light1".sendCommand(ON) // sends outgoing command to device
        // other actions are:
        //      "Door1".updateState(CLOSED) // updates state at server only
        
        // yet another way of sending command. kept for backwards compatibility to RulesDSL
        sendCommand("Light2", ON) 
        // other backwards compatible actions are:
        //      updateState("Door1", CLOSED)

        // handle collection based actions using LAMBDA expression
        // Imagine systemConfig to be an entry point into configuration data, 
        //     the exact structure will be documented soon
        // You can have multiple actions inside forEach { }
        systemConfig.emergencyPersonal.filter(it.name == "Jack" || it.name == "Kim").forEach { sendSMS(it.phone, msg) }

        // lookup and use OSGI service, with special systemService helper
        val jsonStore = systemService<StorageService>()
        var myStorage: Storage<Int> = jsonStore.getStorage("MyStore")
        
        // null safety made easy
        // if old alertCount found, increment it, else initialize with 1
        var alertCount = myStorage.get("AlertCount")?.plus(1) : 1;
        myStorage.put("AlertCount", alertCount)
    }
    
    // there could be multiple actions clauses
}

// common offline test setup, executed freshly before every offline test
commonOfflineTestSetup {
    "MotionSensor1".addAsTestThing("binding2:gateway1:motion:MotionSensor1")
    "Light1".addAsTestItem(OnOffType.java::class)
    "Light2".addAsTestItem(OnOffType.java::class)
    "Door1".addAsTestChannel(OpenClosedType.java::class)
}

offlineTest "Scenario1" {
    // test specific setup script. executed after common-offline-test-setup
    setup {
        println("Running Scenario1")
    }
    
    // test main body
    actions {
        "MotionSensor1".updateState(ONLINE)
        "Light1".sendCommand(ON)
        "Door1".updateState(CLOSED)
    }
    // there could be multiple actions clauses
    
    // test assertions
    assertIf { 
        "My Kotlin Rule1".isNotTriggered && "My Kotlin Rule2".isNotTriggered 
    }
    // there could be multiple assert clauses
}

offlineTest "Scenario2" {
    setup {
        println("Running Scenario2")
    }
    
    actions {
        "MotionSensor1".updateState(OFFLINE)
        "Light1".sendCommand(ON)
        "Door1".updateState(CLOSED)
        delay(0.5f)
        "Door1".updateState(OPEN)
    }
    
    assertIf {
        "My Kotlin Rule1".isTriggered && "Light2".is(ON)
    }
    
    assertIf { "My Kotlin Rule2".isNotTriggered }
}
