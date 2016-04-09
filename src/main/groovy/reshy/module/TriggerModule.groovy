package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command
import reshy.data.Quote
import reshy.util.SpreadsheetConnector

class TriggerModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    private SpreadsheetConnector triggerData
    private SpreadsheetConnector usedData

    private Map classes

    void init() {
        name = 'trigger'
        helpMessage = 'Provides functionality dealing with generating capes from triggers.'
        commands = [
            [name: 'trigger', mode: AccessMode.ENABLED, triggers: ['~triggertest'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> getTrigger(*msc) },
                helpMessage: 'Grabs a trigger event from the spreadsheet of trigger events.'
            ] as Command,
            [name: 'used', mode: AccessMode.ENABLED, triggers: ['~usedtest'], on: [Action.MESSAGE],
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
        triggerData = new SpreadsheetConnector('Weaver Dice Triggers', 'Trigger Events')
        String error = triggerData.init(bot.getOptions().googleOAuthJson)
        if(error) {
            bot.send(bot.owner(), "triggerData: $error")
        }
        usedData = new SpreadsheetConnector('Weaver Dice Triggers', 'Used Triggers')
        error = usedData.init(bot.getOptions().googleOAuthJson)
        if(error) {
            bot.send(bot.owner(), "triggerData: $error")
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
                bot.send((options.pm != null) ? sender : channel, 'No used trigger with that number')
                return
            }
            selection = options.number
        }
        else {
            selection = (Math.random() * triggers.size()) as int
        }
        bot.send((options.pm != null) ? sender : channel, "${sender} - Used trigger ${selection}: ${triggers[selection]}")
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
}