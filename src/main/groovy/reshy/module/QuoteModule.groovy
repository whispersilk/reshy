package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command
import reshy.data.Quote

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class QuoteModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    private static final String DEFAULT_PATH_TO_FILE = System.getProperty('user.dir')
    private static final String DEFAULT_FILE = 'quotes.json'
    private File file

    private static final List QUOTES = []

    void init() {
        name = 'quote'
        helpMessage = 'Provides functionality for storing, finding, and retrieving quotes.'
        commands = [
            [name: 'addquote', mode: AccessMode.ENABLED, triggers: ['~addquote'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> addQuote(*msc) },
                helpMessage: 'Adds a quote to the list of quotes. Invoked as [trigger] [quote]'
            ] as Command,
            [name: 'delquote', mode: AccessMode.ENABLED, triggers: ['~delquote'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> removeQuote(msc[0], msc[2]) },
                helpMessage: 'Removes the quote at the given position from the list of quotes. Invoked as [trigger] [position]'
            ] as Command,
            [name: 'findquote', mode: AccessMode.ENABLED, triggers: ['~findquote'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> findQuote(msc[0], msc[2]) },
                helpMessage: 'Finds all quotes containing the given text. Invoked as [trigger] [text]'
            ] as Command,
            [name: 'quote', mode: AccessMode.ENABLED, triggers: ['~quote'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> getQuote(msc[0], msc[2]) },
                helpMessage: 'Returns the quote at the given index from the list of quotes - if no index is given, returns a random quote. Invoked as [trigger] [optional index]'
            ] as Command,
            [name: 'numquotes', mode: AccessMode.ENABLED, triggers: ['~numquotes'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> numQuotes(msc[2]) },
                helpMessage: 'Returns the number of quotes in the list. Invoked as [trigger]'
            ] as Command,
            [name: 'whoquote', mode: AccessMode.ENABLED, triggers: ["~who"], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> whoQuote(msc[0], msc[2]) },
                helpMessage: 'Returns the nick of the user who added the quote to the given index. Invoked as [trigger] [index]'
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

    void addQuote(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String quote = pieces.join(' ')
        QUOTES << ([quote: quote, sender: sender] as Quote)
        saveQuotes()
        bot.send(channel, "Quote added in position ${QUOTES.size() - 1}.")
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
        if(index >= 0 && index < QUOTES.size()) {
            Quote quote = QUOTES.remove(index)
            saveQuotes()
            bot.send(channel, "Quote \"${quote.quote}\" removed from position ${index}.")
        }
        else {
            bot.send(channel, "Quote not found for index ${index}.")
        }
    }

    void findQuote(String message, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String piece = pieces.join(' ')
        String replyQuote
        List indeces = []
        QUOTES.eachWithIndex { quoteObject, index ->
            String quote = quoteObject.quote
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
            index = (Math.random() * QUOTES.size()) as int
        }
        else {
            try {
                index = position as int
            }
            catch(NumberFormatException e) {
                bot.send(channel, 'Quote index needs to be a number.')
                return
            }
        }
        bot.send(channel, "${QUOTES[index] ? "Quote ${index}: ${QUOTES[index].quote}" : ''}")
    }

    void numQuotes(String channel) {
        bot.send(channel, "${QUOTES.size()} quotes.")
    }

    void whoQuote(String message, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String indexString = pieces.join(' ')
        int index
        try {
            index = indexString as int
        }
        catch(NumberFormatException e) {
            bot.send(channel, 'Position to find author of must be an integer.')
            return
        }
        if(index >= 0 && index < QUOTES.size()) {
            Quote quote = QUOTES.get(index)
            bot.send(channel, "Quote ${index} added by: ${quote.sender}.")
        }
        else {
            bot.send(channel, "Quote not found for index ${index}.")
        }
    }

    void saveQuotes() {
        file.delete()
        file.createNewFile()
        file.withWriter { writer ->
            writer.write JsonOutput.prettyPrint(JsonOutput.toJson(QUOTES))
        }
    }

    void loadQuotes() {
        QUOTES.clear()
        String jsonString = file.collect { it }.join(' ')
        QUOTES.addAll(new JsonSlurper().parseText(jsonString).collect { it as Quote })
    }
}