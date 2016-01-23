package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command

class QuoteModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    private static final String DEFAULT_PATH_TO_FILE = System.getProperty('user.dir')
    private static final String DEFAULT_FILE = 'quotes.txt'
    private File file

    private static final List quotes = []

    void initCommands() {
        commands = [
            [name: 'addquote', mode: AccessMode.ENABLED, triggers: ['~addquote'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> addQuote(*msc) }
            ] as Command,
            [name: 'delquote', mode: AccessMode.ENABLED, triggers: ['~delquote'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> removeQuote(msc[0], msc[2]) }
            ] as Command,
            [name: 'findquote', mode: AccessMode.ENABLED, triggers: ['~findquote'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> findQuote(msc[0], msc[2]) }
            ] as Command,
            [name: 'quote', mode: AccessMode.ENABLED, triggers: ['~quote'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> getQuote(msc[0], msc[2]) }
            ] as Command,
            [name: 'numquotes', mode: AccessMode.ENABLED, triggers: ['~numquotes'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> numQuotes(msc[2]) }
            ] as Command
        ]
    }

    String name() {
        return 'quote'
    }

    boolean registers(Action action) {
        return (action in REGISTERS)
    }

    void setup(ReshBot bot) {
        this.bot = bot
        this.options = bot.getOptions().quote
        this.mode = options?.mode ? AccessMode.fromString(options.mode) : AccessMode.ENABLED
        String filePath = options?.filepath ?: DEFAULT_PATH_TO_FILE
        String fileName = options?.filename ?: DEFAULT_FILE
        file = new File(filePath, fileName)
        file.createNewFile()
        loadQuotes()
        commands.each { command -> 
            Map entry = options?.commands[command.name]
            if(entry) {
                command.mode = AccessMode.fromString(entry.mode) ?: command.mode
                command.triggers = (entry.triggers != null) ? entry.triggers as Set : command.triggers
                command.on = (entry.on != null) ? entry.on.collect { Action.fromString(it) } as Set : command.on
            }
        }
    }

    Map getSettings() {
        Map settings = [mode: AccessMode.toString(this.mode), filepath: options.filepath, filename: options.filename]
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

    void addQuote(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String quote = pieces.join(' ')
        file << "$quote\n"
        quotes << quote
        bot.send(channel, "Quote added in position ${quotes.size() - 1}.")
    }

    void removeQuote(String message, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String indexString = pieces.join(' ')
        int index
        try {
            index = indexString as int
        }
        catch(NumberFormatException e) {
            bot.send(channel, 'Position to remove must be an integer.')
            return
        }
        if(index >= 0 && index < quotes.size()) {
            String quote = quotes.remove(index)
            saveQuotes()
            bot.send(channel, "Quote \"${quote}\" removed from position ${index}.")
        }
        bot.send(channel, "Quote not found for index ${index}.")
    }

    void findQuote(String message, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String piece = pieces.join(' ')
        String replyQuote
        List indeces = []
        quotes.eachWithIndex { quote, index ->
            if(!replyQuote && quote.contains(piece)) {
                replyQuote = quote
                indeces << index
            }
            else if(quote.contains(piece)) {
                indeces << index
            }
        }
        if(!replyQuote) {
            bot.send(channel, "Sorry, no quotes contain that.")
        }
        String firstFind = "At position ${indeces.remove(0)}: ${replyQuote}"
        String otherFinds = (indeces) ? "\nAlso found in [${indeces.join(', ')}]." : ''
        bot.send(channel, "${firstFind}${otherFinds}")
    }

    void getQuote(String message, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String position = pieces.join(' ')
        int index
        if(position == '') {
            index = (Math.random() * quotes.size()) as int
        }
        else {
            try {
                index = position as int
            }
            catch(NumberFormatException e) {
                bot.send(channel, 'Quote index needs to be a number.')
            }
        }
        bot.send(channel, "${quotes[index] ? "Quote $index - ${quotes[index]}" : ''}")
    }

    void numQuotes(String channel) {
        bot.send(channel, "${quotes.size()} quotes.")
    }

    void loadQuotes() {
        quotes.clear()
        quotes.addAll(file.collect { it })
    }
}