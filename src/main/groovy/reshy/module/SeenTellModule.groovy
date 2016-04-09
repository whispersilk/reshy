package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command
import reshy.data.Seen
import reshy.data.Tell
import reshy.util.FileConnector

class SeenTellModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.JOIN, Action.MESSAGE, Action.PRIVATEMESSAGE]

    private static final String DEFAULT_PATH_TO_FILES = System.getProperty('user.dir')
    private static final String DEFAULT_SEEN_FILE = 'seens.json'
    private static final String DEFAULT_TELL_FILE = 'tells.json'
    private FileConnector seenFile
    private FileConnector tellFile

    private static final List SEENS = []
    private static final List TELLS = []

    void init() {
        name = 'seentell'
        helpMessage = 'Provides functionality for checking when users were last seen, as well as for sending messages to be delivered to users in the future.'
        commands = [
            [name: 'seen', mode: AccessMode.ENABLED, triggers: ['~seen'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doSeen(*msc) },
                helpMessage: 'Returns the last time a user was seen, and what they were seen saying. Invoked as [trigger] [nick]'
            ] as Command,
            [name: 'tell', mode: AccessMode.ENABLED, triggers: ['~tell'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doTell(*msc) },
                helpMessage: 'Stores a message to be told to a user at a later time. Invoked as [trigger] [recipient nick] [message]'
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
        String filePath = options?.filepath ?: DEFAULT_PATH_TO_FILES
        String seenFileName = options?.seenfilename ?: DEFAULT_SEEN_FILE
        String tellFileName = options?.tellfilename ?: DEFAULT_TELL_FILE
        seenFile = new FileConnector(filePath, seenFileName)
        tellFile = new FileConnector(filePath, tellFileName)
        SEENS.clear()
        SEENS.addAll(seenFile.load('list').collect { Seen.fromMap(it) })
        TELLS.clear()
        TELLS.addAll(tellFile.load('list').collect { it as Tell })
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
        Map settings = [mode: AccessMode.toString(this.mode), filepath: options.filepath, seenfilename: options?.seenfilename ?:DEFAULT_SEEN_FILE, tellfilename: options?.tellfilename ?: DEFAULT_TELL_FILE]
        Map commandMap = [:]
        commands.each { command ->
            List actions = command.on.collect { Action.toString(it) }
            commandMap.put(command.name, [mode: AccessMode.toString(command.mode), triggers: command.triggers as List, on: actions])
        }
        settings.put('commands', commandMap)
        return settings
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
        if(sender != bot.getNick()) {
            update(channel, sender, "${sender} has joined ${channel}")
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
        update(channel, sender, message)
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

    void update(String channel, String sender, String message) {
        updateSeens(channel, sender, message)
        tellIfNeeded(channel, sender)
    }

    void updateSeens(String channel, String sender, String message) {
        Seen seen = SEENS.find { seen -> 
            seen.user == sender
        }
        if(!seen) {
            SEENS << ([user: sender, channel: channel, message: message, time: Calendar.getInstance()] as Seen)
        }
        else {
            seen.channel = channel
            seen.message = message
            seen.time = Calendar.getInstance()
        }
        seenFile.save(SEENS.collect { Seen.toMap(it) })
    }

    void tellIfNeeded(String channel, String sender) {
        List tells = TELLS.findAll { tell ->
            tell.recipient.equalsIgnoreCase(sender)
        }
        List messages = tells.collect { it.print() }
        if(messages) {
            String send = "${sender}:\n|   "
            send += messages.join('\n|   ')
            bot.send(channel, send)
            tells.each { message ->
                TELLS.remove(message)
            }
            tellFile.save(TELLS)
        }
    }

    void doSeen(String message, String sender, String channel) {
        List pieces = message.split(' ')
        String user = pieces[1]
        if(user.equalsIgnoreCase(bot.getNick())) {
            bot.send(channel, "I'm here right now!")
        }
        else if(user.equalsIgnoreCase(sender)) {
            bot.send(channel, "You're right here!")
        }
        else {
            Seen seen = SEENS.find { seen ->
                seen.user?.equalsIgnoreCase(user)
            }
            if(seen) {
                bot.send(channel, seen.print())
            }
            else {
                bot.send(channel, "Sorry, I haven't seen that user.")
            }
        }
    }

    void doTell(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String recipient = pieces.remove(0)
        if(recipient.equalsIgnoreCase(bot.getNick())) {
            bot.send(channel, "I hear you loud and clear!")
        }
        else if(recipient == sender) {
            bot.send(channel, "You don't need me to tell you that!")
        }
        else {
            String note = pieces.join(' ')
            Tell tell = [sender: sender, recipient: recipient, message: note] as Tell
            TELLS << tell
            tellFile.save(TELLS)
            bot.send(channel, "I will tell them that!")
        }
    }
}