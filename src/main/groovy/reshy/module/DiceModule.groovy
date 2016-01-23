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

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    void initCommands() {
        commands = [
            [name: 'roll', mode: AccessMode.ENABLED, triggers: ['~roll'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String ... msc -> doRoll(msc[0], msc[2]) }
            ] as Command,
            [name: 'add', mode: AccessMode.ENABLED, triggers: ['~add'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String ... msc -> doAdd(msc[0], msc[2]) }
            ] as Command
        ]
    }

    String name() {
        return 'dice'
    }

    boolean registers(Action action) {
        return (action in REGISTERS)
    }

    void setup(ReshBot bot) {
        this.bot = bot
        this.options = bot.getOptions().dice
        this.mode = AccessMode.fromString(options.mode)
        commands.each { command -> 
            Map entry = options.commands[command.name]
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

    void doRoll(String message, String channel) {
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
        (rolls, sides, error) = getNumAndSides(numAndSides)
        if(error) {
            bot.send(channel, error)
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
        List results = []
        for(int x = 0; x < rolls; x++) {
            results << ((Math.random() * sides + 1 + mod * mult) as int)
        }
        bot.send(channel, "[${results.join(', ')}]${tag ? ' - Tag: ' + tag : ''}")
    }

    void doAdd(String message, String channel) {
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
        (rolls, sides, error) = getNumAndSides(numAndSides)
        if(error) {
            bot.send(channel, error)
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
        bot.send(channel, "[${result}]${tag ? ' - Tag: ' + tag : ''}")
    }

    List getNumAndSides(String numAndSides) {
        int num, sides
        String error
        List vals = numAndSides.split('d') as List
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