package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command
import reshy.data.Quote
import reshy.util.SpreadsheetConnector

import groovy.json.JsonSlurper

class TriggerModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    private Map futharkData
    private SpreadsheetConnector triggerData
    private SpreadsheetConnector usedData
    private SpreadsheetConnector luckData

    private Map classes

    void init() {
        name = 'trigger'
        helpMessage = 'Provides functionality dealing with generating capes from triggers.'
        commands = [
            [name: 'trigger', mode: AccessMode.ENABLED, triggers: ['~trigger'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> getTrigger(*msc) },
                helpMessage: 'Grabs a trigger event from the spreadsheet of trigger events.'
            ] as Command,
            [name: 'used', mode: AccessMode.ENABLED, triggers: ['~used'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> getUsed(*msc) },
                helpMessage: 'Grabs a trigger event from the spreadsheet of used trigger events.'
            ] as Command,
            [name: 'numtriggers', mode: AccessMode.ENABLED, triggers: ['~numtriggers'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> numTriggers(msc[2]) },
                helpMessage: 'Grabs the number of trigger events.'
            ] as Command,
            [name: 'numused', mode: AccessMode.ENABLED, triggers: ['~numused'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> numUsed(msc[2]) },
                helpMessage: 'Grabs the number of used trigger events.'
            ] as Command,
            [name: 'futhark', mode: AccessMode.ENABLED, triggers: ['~futhark'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> futhGen(*msc) },
                helpMessage: 'Gives information for futhark power generation.'
            ] as Command,
            [name: 'luck', mode: AccessMode.ENABLED, triggers: ['~luck', '~perk', '~flaw'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> getLuck(*msc) },
                helpMessage: 'Rolls luck for a power.'
            ] as Command
        ]
    }

    boolean registers(Action action) {
        return (action in REGISTERS)
    }

    void setup(ReshBot bot) {
        this.bot = bot
        this.options = bot.getOptions()[name]
        this.mode = options?.mode ? AccessMode.fromString(options.mode) : AccessMode.ENABLED
        futharkData = new JsonSlurper().parseText(this.getClass().getResource('/futhgendata.json').getText())
        triggerData = new SpreadsheetConnector('Weaver Dice Triggers', 'Trigger Events')
        String error = triggerData.init(bot.getOptions().bot.googleOAuthJson)
        if(error) {
            bot.send(bot.owner(), "triggerData: $error")
        }
        usedData = new SpreadsheetConnector('Weaver Dice Triggers', 'Used Triggers')
        error = usedData.init(bot.getOptions().bot.googleOAuthJson)
        if(error) {
            bot.send(bot.owner(), "usedData: $error")
        }
        luckData = new SpreadsheetConnector('Detail Generator Clone', 'LUCK')
        error = luckData.init(bot.getOptions().bot.googleOAuthJson)
        if(error) {
            bot.send(bot.owner(), "luckData: $error")
        }
        commands.each { command -> 
            Map entry = options?.commands ? options.commands[command.name] : null
            if(entry) {
                command.mode = AccessMode.fromString(entry.mode) ?: command.mode
                command.triggers = (entry.triggers != null) ? entry.triggers as Set : command.triggers
                command.on = (entry.on != null) ? entry.on.collect { Action.fromString(it) } as Set : command.on
            }
        }
    }

    Map getSettings() {
        Map settings = [mode: AccessMode.toString(this.mode)]
        Map commandMap = [:]
        commands.each { command ->
            List actions = command.on.collect { Action.toString(it) }
            commandMap.put(command.name, [mode: AccessMode.toString(command.mode), triggers: command.triggers as List, on: actions])
        }
        settings.put('commands', commandMap)
        return settings
    }

    void onMessage(String channel, String sender, String login, String hostname, String message) {
        commands.each { command ->
            if(Action.MESSAGE in command.on && command.condition(message)) {
                if(isValid(command, sender)) {
                    command.action(message, sender, channel)
                }
                else if(command.errMessage) {
                    bot.send(channel, command.errMessage)
                }
            }
        }
    }

    void onPrivateMessage(String sender, String login, String hostname, String message) {
        commands.each { command ->
            if(Action.PRIVATEMESSAGE in command.on && command.condition(message)) {
                if(isValid(command, sender)) {
                    command.action(message, sender, sender)
                }
                else if(command.errMessage) {
                    bot.send(sender, command.errMessage)
                }
            }
        }
    }

    void getTrigger(String message, String sender, String channel) {
        List triggers = triggerData.getColumnByName('triggerevents')
        Map options = getExtras(message)
        int selection = -1
        if(options.number != null) {
            if(options.number >= triggers.size() || options.number < 0) {
                bot.send((options.pm) ? sender : channel, 'No trigger with that number')
                return
            }
            selection = options.number
        }
        else {
            selection = (Math.random() * triggers.size()) as int
        }
        bot.send((options.pm) ? sender : channel, "${sender} - Trigger ${selection}: ${triggers[selection]}")
    }

    void getUsed(String message, String sender, String channel) {
        List triggers = usedData.getColumnByName('triggerevents')
        Map options = getExtras(message)
        int selection = -1
        if(options.number != null) {
            if(options.number >= triggers.size() || options.number < 0) {
                bot.send((options.pm) ? sender : channel, 'No used trigger with that number')
                return
            }
            selection = options.number
        }
        else {
            selection = (Math.random() * triggers.size()) as int
        }
        bot.send((options.pm) ? sender : channel, "${sender} - Used trigger ${selection}: ${triggers[selection]}")
    }

    Map getExtras(String message) {
        Map toReturn = [pm: false]
        List pieces = message.split(' ')
        pieces.remove(0)
        if(pieces && pieces[0] =~ /\d+/) {
            toReturn.put('number', pieces[0] as int)
            pieces.remove(0)
        }
        if(pieces && pieces[0].equalsIgnoreCase('pm')) {
            toReturn.pm = true
        }
        return toReturn
    }

    void numTriggers(String channel) {
        bot.send(channel, triggerData.getColumnByName('triggerevents').size() + ' triggers')
    }

    void numUsed(String channel) {
        bot.send(channel, usedData.getColumnByName('triggerevents').size() + ' used triggers')
    }

    void futhGen(String message, String sender, String channel) {
        int classValInt = (Math.random() * futharkData.class.size()) as int
        int flavorValInt = (Math.random() * futharkData.flavor.size()) as int
        int twistValueValInt = (Math.random() * futharkData.twistvalue.size()) as int
        int twistSuitValInt = (Math.random() * futharkData.twistsuit.size()) as int
        Map classVal = futharkData.class[classValInt]
        Map flavorVal = futharkData.flavor[flavorValInt]
        Map twistValueVal = futharkData.twistvalue[twistValueValInt]
        Map twistSuitVal = futharkData.twistsuit[twistSuitValInt]
        bot.send(channel, "You rolled ${classVal.name}. Your flavor is ${flavorVal.name.toLowerCase()} and your twist is the ${twistValueVal.name.toLowerCase()} of ${twistSuitVal.name.toLowerCase()}\n" + 
            "    ${classVal.name}: ${classVal.text}\n    ${flavorVal.name}: ${flavorVal.text}\n    ${twistValueVal.name}: ${twistValueVal.text}\n    ${twistSuitVal.name}: ${twistSuitVal.text}")
    }

    List luckColumns = [[name: 'perklife', printed: 'Life Perk'], [name: 'perkpower', printed: 'Power Perk'], [name: 'flawlife', printed: 'Life Flaw'], [name: 'flawpower', printed: 'Power Flaw']]

    void getLuck(String message, String sender, String channel) {
        if(message.split(' ')[0] == '~perk') {
            getPerk(message, sender, channel)
            return
        }
        if(message.split(' ')[0] == '~flaw') {
            getFlaw(message, sender, channel)
            return
        }
        if(message.split())
        Map roll1 = luckColumns[(Math.random() * 4) as int]
        Map roll2 = luckColumns[(Math.random() * 4) as int]
        List col1 = luckData.getColumnByName(roll1.name).findAll { it != null }
        List col2 = luckData.getColumnByName(roll2.name).findAll { it != null }
        int numSelected1 = (Math.random() * col1.size()) as int
        int numSelected2 = (Math.random() * col2.size()) as int
        String selected1 = col1[numSelected1]
        String selected2 = col2[numSelected2]
        bot.send(channel, "${sender} - [${roll1.printed} $numSelected1]: $selected1\n[${roll2.printed} $numSelected2]: $selected2")
    }

    void getPerk(String message, String sender, String channel) {
        Map roll
        if(message.split(' ').size() > 1 && message.split(' ')[1] == 'life') {
            roll = luckColumns[0]
        }
        else if(message.split(' ').size() > 1 && message.split(' ')[1] == 'power') {
            roll = luckColumns[1]
        }
        else {
            roll = luckColumns[(Math.random() * 2) as int]
        }
        List col = luckData.getColumnByName(roll.name).findAll { it != null }
        int numSelected
        if(message.split(' ').size() > 2 && message.split(' ')[2] =~ /\d+/) {
            numSelected = message.split(' ')[2] as int
            if(numSelected >= col.size() || numSelected < 0) {
                bot.send(channel, "No ${message.split(' ')[1]} perk with that number.")
                return
            }
        }
        else {
            numSelected = (Math.random() * col.size()) as int
        }
        String selected = col[numSelected]
        bot.send(channel, "${sender} - [${roll.printed} $numSelected]: $selected")
    }

    void getFlaw(String message, String sender, String channel) {
        Map roll
        if(message.split(' ').size() > 1 && message.split(' ')[1] == 'life') {
            roll = luckColumns[2]
        }
        else if(message.split(' ').size() > 1 && message.split(' ')[1] == 'power') {
            roll = luckColumns[3]
        }
        else {
            roll = luckColumns[2 + (Math.random() * 2) as int]
        }
        List col = luckData.getColumnByName(roll.name).findAll { it != null }
        int numSelected
        if(message.split(' ').size() > 2 &&  message.split(' ')[2] =~ /\d+/) {
            numSelected = message.split(' ')[2] as int
            if(numSelected >= col.size() || numSelected < 0) {
                bot.send(channel, "No ${message.split(' ')[1]} flaw with that number.")
                return
            }
        }
        else {
            numSelected = (Math.random() * col.size()) as int
        }
        String selected = col[numSelected]
        bot.send(channel, "${sender} - [${roll.printed} $numSelected]: $selected")
    }
}