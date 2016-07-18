package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command

class DiceModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]
    private static final int MAX_ROLLS = 500
    private static final String CHOOSE_SEPARATOR = '\\.|\\||,' // This is a regex string, but special characters must be escaped using \\

    void init() {
        name = 'dice'
        helpMessage = 'Provides functionality for rolling dice.'
        commands = [
            [name: 'roll', mode: AccessMode.ENABLED, triggers: ['~roll'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String ... msc -> doRoll(*msc) },
                helpMessage: 'Rolls a given number of dice with a given number of sides and applys an optional modifier to each roll, then displays the results along with an optional tag. Invoked as [trigger] [num]d[sides] [+/-mod] [tag]'
            ] as Command,
            [name: 'add', mode: AccessMode.ENABLED, triggers: ['~add'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String ... msc -> doAdd(*msc) },
                helpMessage: 'Rolls a given number of dice with a given number of sides and applys an optional modifier to each roll, then displays the sum of the results along with an optional tag. Invoked as [trigger] [num]d[sides] [+/-mod] [tag]'
            ] as Command,
            [name: 'choose', mode: AccessMode.ENABLED, triggers: ['~choose'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String ... msc -> doChoose(*msc) },
                helpMessage: 'Chooses one option from a list. Invoked as [trigger] [option 1] | [option 2] | ... | [option x]'
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
            Map entry = options?.commands[command.name]
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

    void doRoll(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String roll = pieces.join(' ')
        if(!pieces) {
            bot.send(channel, 'To roll you should use [X]dY [+/- mod] [tag]. X, Y, and mod should be integers.')
            return
        }
        String mode = ''
        String numAndSides = roll.find(/^[A-Za-z0-9]*d[A-Za-z0-9]+/)
        if(!numAndSides) {
            numAndSides = roll.find(/^[A-Za-z0-9]*h[A-Za-z0-9]+/)
            roll = roll.replaceFirst(/^[A-Za-z0-9]*h[A-Za-z0-9]+/, '')
            mode = 'h'
        }
        else {
            roll = roll.replaceFirst(/^[A-Za-z0-9]*d[A-Za-z0-9]+/, '')
            mode = 'd'
        }
        int rolls, sides
        String error
        (rolls, sides, error) = getNumAndSides(numAndSides, mode)
        if(error) {
            bot.send(channel, error)
            return
        }
        if(rolls > MAX_ROLLS) {
            bot.send(channel, "Sorry, I won't let you roll more than ${MAX_ROLLS} dice at once.")
            return
        }
        String modAndMult = roll.find(/^[\s]*[\+-][\s]*[A-Za-z0-9]+/)
        roll = roll.replaceFirst(/^[\s]*[\+-][\s]*[A-Za-z0-9]+/, '')
        int mod, mult
        (mod, mult, error) = getModAndMult(modAndMult)
        if(error) {
            bot.send(channel, error)
            return
        }
        String tag = roll.replaceFirst(/^[\s]*/, '')
        if(mode == 'd') {
            showAllRolls(sender, channel, rolls, sides, mod, mult, tag)
        }
        else if(mode == 'h') {
            showHighestRoll(sender, channel, rolls, sides, mod, mult, tag)
        }
    }

    void showAllRolls(String sender, String channel, int rolls, int sides, int mod, int mult, String tag) {
        List results = []
        for(int x = 0; x < rolls; x++) {
            results << ((Math.random() * sides + 1 + mod * mult) as int)
            if(results[results.size() - 1] == sides) {
                results[results.size() - 1] = "" + results[results.size() - 1] + ""
            }
        }
        if(rolls > 30) {
            bot.send(channel, "${sender}, I'm sending the result of the rolls to you directly to avoid cluttering the channel.")
            bot.send(sender, "[${results.join(', ')}]${tag ? ' - Tag: ' + tag : ''}")
        }
        else {
            bot.send(channel, "[${results.join(', ')}]${tag ? ' - Tag: ' + tag : ''}")
        }
    }

    void showHighestRoll(String sender, String channel, int rolls, int sides, int mod, int mult, String tag) {
        int max = 0
        for(int x = 0; x < rolls; x++) {
            int roll = ((Math.random() * sides + 1 + mod * mult) as int)
            if(max < roll) { max = roll }
        }
        bot.send(channel, "Highest of ${rolls}d${sides} is [${max == sides ? '' + max + '' : max}]${tag ? ' - Tag: ' + tag : ''}")
    }

    void doAdd(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String roll = pieces.join(' ')
        if(!pieces) {
            bot.send(channel, 'To roll you should use [X]dY [+/- mod] [tag]. X, Y, and mod should be integers.')
            return
        }
        String numAndSides = roll.find(/^[A-Za-z0-9]*d[A-Za-z0-9]+/)
        roll = roll.replaceFirst(/^[A-Za-z0-9]*d[A-Za-z0-9]+/, '')
        int rolls, sides
        String error
        (rolls, sides, error) = getNumAndSides(numAndSides, 'd')
        if(error) {
            bot.send(channel, error)
            return
        }
        else if(rolls > MAX_ROLLS) {
            bot.send(channel, "Sorry, I won't let you add more than $MAX_ROLLS rolls at once.")
            return
        }
        String modAndMult = roll.find(/^[\s]*[\+-][\s]*[A-Za-z0-9]+/)
        roll = roll.replaceFirst(/^[\s]*[\+-][\s]*[A-Za-z0-9]+/, '')
        int mod, mult
        (mod, mult, error) = getModAndMult(modAndMult)
        if(error) {
            bot.send(channel, error)
            return
        }
        String tag = roll.replaceFirst(/^[\s]*/, '')
        int result
        for(int x = 0; x < rolls; x++) {
            result += ((Math.random() * sides + 1 + mod * mult) as int)
        }
        bot.send(channel, "[${result == rolls * sides ? '' + result + '' : result}]${tag ? ' - Tag: ' + tag : ''}")
    }

    void doChoose(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        List options = pieces.join(' ').split(CHOOSE_SEPARATOR).collect { it.trim() }
        String option = options[(Math.random() * options.size) as int]
        bot.send(channel, "Of those options, I chose: $option")
    }

    List getNumAndSides(String numAndSides, String separator) {
        int num, sides
        String error
        List vals = numAndSides.split(separator) as List
        try {
            num = (vals[0]) ? vals[0] as int : 1
        }
        catch(NumberFormatException e) {
            error = 'Number of rolls needs to be nothing or an integer.'
        }
        try {
            sides = vals[1] as int
        }
        catch(NumberFormatException e) {
            error = 'Number of sides needs to be an integer.'
        }
        return [num, sides, error]
    }

    List getModAndMult(String modAndMult) {
        int mod, mult
        String error
        if(!modAndMult) {
            return [0, 1, error]
        }
        mult = (modAndMult.indexOf('+') > -1) ? 1 : -1
        try {
            String modOnly = modAndMult.replaceFirst(/^[\s]*[\+-][\s]*/, '')
            mod = modOnly as int
        }
        catch(NumberFormatException e) {
            error = 'Modifier value needs to be an integer.'
        }
        return [mod, mult, error]
    }
}