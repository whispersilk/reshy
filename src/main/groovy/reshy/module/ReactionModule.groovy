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

    private static final List PIGS_BLOOD_OPTIONS = ['fetches the buckets.', 'thaws the offerings.', 'prepares the incense.', 'hides the pigs.', 'draws the pentagram.']
    private static final double ODDS_OF_PIGSBLOOD = 0.6

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
            ] as Command/*,
            [name: 'speak', mode: AccessMode.OWNER_ONLY, triggers: ['~speak'], on: [Action.MESSAGE],
                condition: { String message -> delegate.triggers.find { message.split(' ')[0] == it } && message.split(' ').length > 1 },
                action: { String... msc -> sayPhrase(*msc) },
                helpMessage: 'Causes the bot to say the given string in the given channel. Note that attempting to use this command for "/me" actions will fail. Invoked as [trigger] [channel] [text]'
            ] as Command*/,
            [name: 'pigsblood', mode: AccessMode.ENABLED, triggers: [], on: [Action.MESSAGE],
                condition: { String message -> message.matches(/(?i).*pig[']{0,1}s blood.*(?-i)/) },
                action: { String... msc -> pigsBlood(msc[2]) },
                helpMessage: 'The ritual.'
            ] as Command,
            [name: 'ship', mode: AccessMode.ENABLED, triggers: ['~ship'], on: [Action.MESSAGE],
                condition: { String message -> delegate.triggers.find { message.split(' ')[0] == it }},
                action: { String... msc -> findShippingPair(msc[2]) },
                helpMessage: 'Selects two users at random from the current channel (excluding me), and pairs them together. Invoked as [trigger]'
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

    void pigsBlood(String channel) {
        String phrase = PIGS_BLOOD_OPTIONS[(Math.random() * PIGS_BLOOD_OPTIONS.size()) as int]
        double chance = Math.random()
        if(chance <= ODDS_OF_PIGSBLOOD)
        bot.sendAction(channel, "$phrase")
    }

    void findShippingPair(String channel) {
        List users = bot.getUsers(channel) as List
        users = users.findAll { it.getNick() != bot.getNick() }
        def user1 = users[(Math.random() * users.size()) as int]
        users = users - user1
        def user2 = users[(Math.random() * users.size()) as int]
        bot.send(channel, "${user1.getNick()} x ${user2.getNick()} OTP")
    }

/*    void sayPhrase(String message, String sender, String channel) {
        String 
    }*/
}