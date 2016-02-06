package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command
import reshy.data.Quote

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class CapeModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    private static final String DEFAULT_PATH_TO_FILE = System.getProperty('user.dir')
    private static final String DEFAULT_FILE = 'classes.json'
    private File file

    private Map classes

    void init() {
        name = 'cape'
        helpMessage = 'Provides functionality dealing with genning capes.'
        commands = [
            [name: 'class', mode: AccessMode.ENABLED, triggers: ['~mover', '~shaker', '~brute', '~breaker', '~blaster', '~thinker', '~master', '~tinker', '~striker', '~changer', '~trump', '~stranger'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> changeQuantity(msc[0], msc[2]) },
                helpMessage: 'Changes or views the stored quantity of capes with the given classification, to the closest 1/10  . Invoked as [trigger] [number] to change the quantity, or [trigger] to view the quantity'
            ] as Command,
            [name: 'classes', mode: AccessMode.ENABLED, triggers: ['~classes'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> viewQuantities(msc[2]) },
                helpMessage: 'Views the number of capes with each classification. Invoked as [trigger]'
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
        String filePath = options?.filepath ?: DEFAULT_PATH_TO_FILE
        String fileName = options?.filename ?: DEFAULT_FILE
        file = new File(filePath, fileName)
        file.createNewFile()
        loadClasses()
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
        Map settings = [mode: AccessMode.toString(this.mode), filepath: options?.filepath ?: DEFAULT_PATH_TO_FILE, filename: options?.filename ?: DEFAULT_FILE]
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

    void changeQuantity(String message, String channel) {
        List pieces = message.split(' ')
        String classification = pieces.remove(0) - '~'
        if(pieces.size() == 0) {
            bot.send(channel, "Current value for ${classification}: ${classes[classification] ?: 0}")
            return
        }
        double quantity
        try {
            quantity = Double.parseDouble(pieces.remove(0))
        }
        catch(NumberFormatException e) {
            bot.send(channel, 'Quantity by which to increment classification must be a number.')
            return
        }
        if(classes[classification]) {
            classes[classification] = Math.round((classes[classification] + quantity) * 10) / 10.0
        }
        else {
            classes[classification] = Math.round(quantity * 10) / 10.0
        }
        classes[classification] = (classes[classification] > 0) ? classes[classification] : 0
        saveClasses()
        String classificationName = capitalizeFirstLetter(classification)
        bot.send(channel, "${classificationName} count changed by ${quantity}. Current value: ${ (classes[classification] == (classes[classification] as int)) ? (classes[classification] as int) : classes[classification] }")
    }

    void viewQuantities(String channel) {
        List values = []
        classes.each { key, value ->
            values << "${capitalizeFirstLetter(key)}: ${ (value == (value as int)) ? (value as int) : value }"
        }
        bot.send(channel, values.join(' | '))
    }

    String capitalizeFirstLetter(String classification) {
        String toReturn = classification
        if(Character.isLetter(classification[0] as char)) {
            toReturn = classification.replace(classification[0] as char, Character.toUpperCase(classification[0] as char))
        }
        return toReturn
    }

    void saveClasses() {
        file.delete()
        file.createNewFile()
        file.withWriter { writer ->
            writer.write JsonOutput.prettyPrint(JsonOutput.toJson(classes))
        }
    }

    void loadClasses() {
        String jsonString = file.collect { it }.join(' ')
        if(!jsonString) {
            classes = [:]
            return
        }
        classes = new JsonSlurper().parseText(jsonString)
    }
}