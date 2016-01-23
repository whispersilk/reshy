package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command

class ReactionModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass

    private static final List REGISTERS = [Action.ACTION, Action.JOIN, Action.MESSAGE, Action.PRIVATEMESSAGE]

    void initCommands() {
        commands = [
            [name: 'greet', mode: AccessMode.ENABLED, triggers: [], on: [Action.JOIN],
            condition: null,
            action: { String... sc -> sendHello(*sc) }
            ] as Command,
            [name: 'hello', mode: AccessMode.ENABLED, triggers: ['~hello', 'hi', 'hello', 'hey', 'hiho', 'oi'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
            condition: { String message -> delegate.triggers.find { it.equalsIgnoreCase(message.split(' ')[0]) } && triggerHello(message) },
            action: { String... msc -> sendHello(msc[1], msc[2]) }
            ] as Command,
            [name: 'farewell', mode: AccessMode.ENABLED, triggers: ['~bye', 'bye', 'later', 'seeya', 'goodbye', 'night'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
            condition: {String message -> delegate.triggers.find { it.equalsIgnoreCase(message.split(' ')[0]) } && triggerHello(message) }, // We can just use triggerHello again because the logic is the same.
            action: { String... msc -> sendGoodbye(msc[1], msc[2]) }
            ] as Command
        ]
    }

    String name() {
        return 'reaction'
    }

    boolean registers(Action action) {
        return (action in REGISTERS)
    }

    void setup(ReshBot bot) {
        this.bot = bot
        this.options = bot.getOptions().reaction
        this.mode = options?.mode ? AccessMode.fromString(options.mode) : AccessMode.ENABLED
        commands.each { command -> 
            Map entry = (options?.commands) ? options.commands[command.name] : null
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

    void onAction(String sender, String login, String hostname, String target, String action) {
        commands.each { command ->
            if(Action.ACTION in command.on && command.condition(action)) {
                if(isValid(command, sender)) {
                    command.action(action, sender, target)
                }
                else if(command.errMessage) {
                    bot.send(target, command.errMessage)
                }
            }
        }
    }

    void onJoin(String channel, String sender, String login, String hostname) {
        commands.each { command ->
            if(Action.JOIN in command.on && sender != bot.getName()) {
                if(isValid(command, sender)) {
                    command.action(sender, channel)
                }
                else if(command.errMessage) {
                    bot.send(channel, command.errMessage)
                }
            }
        }
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

    boolean triggerHello(String message) {
        if(message[0] == '~') { return true }
        else if(message.split(' ').size() > 1 && message.split(' ')[1] =~ /(?i)${bot.getName()}(?-i)[\.!]*/) { return true }
        return false
    }

    void sendHello(String sender, String channel) {
        bot.send(channel, "Hello ${sender}!")
    }

    void sendGoodbye(String sender, String channel) {
        bot.send(channel, "Goodbye ${sender}!")
    }
}