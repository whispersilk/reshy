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
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.ACTION, Action.JOIN, Action.MESSAGE, Action.PRIVATEMESSAGE]

    private static final List HAPPY_FACES = [':)', ':D', '^-^', '^.^', '^o^', '^^']
    private static final List SAD_FACES = [':(', 'D:', ';-;', ';^;', ';~;']

    void init() {
        name = 'reaction'
        helpMessage = 'Provides various for interacting directly with users. Not particularly useful, but fun.'
        commands = [
            [name: 'greet', mode: AccessMode.ENABLED, triggers: [], on: [Action.JOIN],
                condition: null,
                action: { String... sc -> sendHello(*sc) },
                helpMessage: 'Greets a user when they join a channel. Not invoked.'
            ] as Command,
            [name: 'hello', mode: AccessMode.ENABLED, triggers: ['~hello', 'hi', 'hello', 'hey', 'hiho', 'oi', 'heyo'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.triggers.find { message =~ /^(?i)${it}[,]{0,1}[\s]+${bot.getNick()}[\.!]*(?-i)$/ || (message.split(' ')[0] == it && it[0] == '~') } },
                action: { String... msc -> sendHello(msc[1], msc[2]) },
                helpMessage: 'Says hello to a user. Invoked as [trigger], or as [trigger (with or without ~)] [bot nick]'
            ] as Command,
            [name: 'farewell', mode: AccessMode.ENABLED, triggers: ['~bye', 'bye', 'later', 'seeya', 'goodbye', 'night'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.triggers.find { message =~ /^(?i)${it}[,]{0,1}[\s]+${bot.getNick()}[\.!]*(?-i)$/ || (message.split(' ')[0] == it && it[0] == '~') } },
                action: { String... msc -> sendGoodbye(msc[1], msc[2]) },
                helpMessage: 'Says goodbye to a user. Invoked as [trigger], or as [trigger (with or without ~)] [bot nick]'
            ] as Command,
            [name: 'pat', mode: AccessMode.ENABLED, triggers: ['~pat', '~headpat', '~pats', '~headpats'], on: [Action.ACTION, Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.triggers.find { message.split(' ')[0] == it || message =~ /^(?i)${it - '~'}[\s]+${bot.getNick()}[\.!]*(?-i)$/} },
                action: { String... msc -> sendHappyFace(msc[2]) },
                helpMessage: 'Returns a random happy face in response to an action. Can be invoked from /me. Invoked as [trigger], or as [trigger (with or without ~)] [bot nick]'
            ] as Command,
            [name: 'scold', mode: AccessMode.ENABLED, triggers: ['~bad', '~scolds'], on: [Action.ACTION, Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.triggers.find { message.split(' ')[0] == it || message =~ /^(?i)${it - '~'}[\s]+${bot.getNick()}[\.!]*(?-i)$/} },
                action: { String... msc -> sendSadFace(msc[2]) },
                helpMessage: 'Returns a random sad face in response to an action. Can be invoked from /me. Invoked as [trigger], or as [trigger (with or without ~)] [bot nick]'
            ] as Command,
            [name: 'reciprocate', mode: AccessMode.ENABLED, triggers: ['~knuffles', '~snuggles', '~cuddles', '~high-fives'], on: [Action.ACTION, Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> (delegate.triggers.find { message.split(' ')[0] == it } && message.split(' ').length == 1) || delegate.triggers.find { message =~ /^(?i)${it - '~'}[\s]+${bot.getNick()}[\.!]*(?-i)$/ } },
                action: { String... msc -> reciprocate(*msc) },
                helpMessage: 'Reciprocates an action on the list of triggers. Can be invoked from /me. Invoked as [trigger], or as [trigger (with or without ~)] [bot nick]'
            ] as Command,
            [name: 'doto', mode: AccessMode.ENABLED, triggers: ['~knuffles', '~snuggles', '~cuddles'], on: [Action.MESSAGE],
                condition: { String message -> delegate.triggers.find { message.split(' ')[0] == it } && message.split(' ').length > 1 },
                action: { String... msc -> doTo(*msc) },
                helpMessage: 'Does an action on the list of triggers to a user in the current channel. Invoked as [trigger] [nick]'
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

    void sendHappyFace(String channel) {
        bot.send(channel, HAPPY_FACES[Math.random() * HAPPY_FACES.size() as int])
    }

    void sendSadFace(String channel) {
        bot.send(channel, SAD_FACES[Math.random() * SAD_FACES.size() as int])
    }

    void reciprocate(String message, String sender, String channel) {
        String action = message.split(' ')[0] - '~'
        bot.sendAction(channel, "${action} ${sender}.")
    }

    void doTo(String message, String sender, String channel) {
        String action, user
        (action, user) = message.split(' ')
        if(bot.getUsers(channel).find { user.equalsIgnoreCase(it.getNick() - '~') } ) {
            if(user.equalsIgnoreCase(bot.getNick())) {
                reciprocate(message, sender, channel)
            }
            else {
                bot.sendAction(channel, "${action - '~'} ${user}.")
            }
        }
    }
}