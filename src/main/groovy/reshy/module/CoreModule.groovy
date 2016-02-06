package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command

class CoreModule extends Module {

    // bot inherited from superclass
    // options inherited from superclass
    // mode inherited from superclass
    // commands inherited from superclass
    // name inherited from superclass
    // helpMessage inherited from superclass

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    void init() {
        name = 'core'
        helpMessage = 'Provides core functionality related to modifying other modules and their permissions.'
        commands = [
            [name: 'join', mode: AccessMode.RESTRICTED, triggers: ['~join'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doJoin(msc[0]) },
                helpMessage: 'Causes the bot to join a channel. Invoked as [trigger] [channel name]'
            ] as Command,
            [name: 'leave', mode: AccessMode.RESTRICTED, triggers: ['~leave'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc ->  doLeave(msc[0]) },
                helpMessage: 'Causes the bot to leave a channel. Invoked as [trigger] [channel name]'
            ] as Command,
            [name: 'enable', mode: AccessMode.RESTRICTED, triggers: ['~enable'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doEnable(*msc) },
                helpMessage: 'Enables a module or command. Invoked as [trigger] [module name] [command name]'
            ] as Command,
            [name: 'restrict', mode: AccessMode.RESTRICTED, triggers: ['~restrict'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doRestrict(*msc)},
                helpMessage: 'Restricts access to a module or command. Invoked as [trigger] [module name] [command name]'
            ] as Command,
            [name: 'disable', mode: AccessMode.RESTRICTED, triggers: ['~disable'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doDisable(*msc)},
                helpMessage: 'Disables a module or command. Invoked as [trigger] [module name] [command name]'
            ] as Command,
            [name: 'save', mode: AccessMode.RESTRICTED, triggers: ['~save'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doSave(msc[2])},
                helpMessage: 'Saves the current settings to the configuration file. Invoked as [trigger]'
            ] as Command,
            [name: 'reload', mode: AccessMode.RESTRICTED, triggers: ['~reload'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doReload(msc[2])},
                helpMessage: 'Reloads the bot\'s modules from the config file. Invoked as [trigger]'
            ] as Command,
            [name: 'addadmin', mode: AccessMode.OWNER_ONLY, triggers: ['~addadmin'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAddAdmin(*msc)},
                helpMessage: 'Adds a user as an admin. Invoked as [trigger] [nick]'
            ] as Command,
            [name: 'deladmin', mode: AccessMode.OWNER_ONLY, triggers: ['~deladmin'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doDelAdmin(*msc)},
                helpMessage: 'Removes a user as an admin. Invoked as [trigger] [nick]'
            ] as Command,
            [name: 'admins', mode: AccessMode.RESTRICTED, triggers: ['~admins'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAdmins(msc[2]) },
                helpMessage: 'Gets a list of the current admins. Invoked as [trigger]'
            ] as Command,
            [name: 'alias', mode: AccessMode.RESTRICTED, triggers: ['~alias'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAlias(*msc) },
                helpMessage: 'Adds an alias as a trigger to a command. Invoked as [trigger] [module] [command] [alias to add]'
            ] as Command,
            [name: 'dealias', mode: AccessMode.RESTRICTED, triggers: ['~dealias'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doDeAlias(*msc) },
                helpMessage: 'Removes an alias as a trigger to a command. Invoked as [trigger] [module] [command] [alias to remove]'
            ] as Command,
            [name: 'aliases', mode: AccessMode.RESTRICTED, triggers: ['~aliases'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAliases(*msc) },
                helpMessage: 'Gets a list off aliases for a command. Invoked as [trigger] [module] [command]'
            ] as Command,
            [name: 'help', mode: AccessMode.ENABLED, triggers: ['~help'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doHelp(*msc) },
                helpMessage: 'Provides help menu information about a module or command. Invoked as [trigger] to display a list of modules, ' + 
                '[trigger] [module] to display a module\'s help information and a list of its commands, or [trigger] [module] [command] to display a command\'s help information.'
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

    void doJoin(String message) {
        String channel = message.split(' ')[1]
        bot.joinChannel(channel)
    }

    void doLeave(String message) {
        String channel = message.split(' ')[1]
        bot.partChannel(channel)
    }

    void doEnable(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded.")
            return
        }
        if(module.name() == name()) {
            bot.send(channel, "Module '${name()}' and its commands may not have their permissions modified.")
            return
        }
        if(pieces.size() == 1) {
            module.mode = AccessMode.ENABLED
            bot.send(channel, "Module '${module.name()}' enabled.")
            return
        }
        Command command = module.commands.find { it.name == pieces[1] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            command.mode = AccessMode.ENABLED
            bot.send(channel, "Command '${command.name}' of module '${module.name()}' enabled.")
        }
        else {
            bot.send(channel, "You can't modify the permissions of that command.")
        }
    }

    void doRestrict(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded.")
            return
        }
        if(module.name() == name()) {
            bot.send(channel, "Module '${name()}' and its commands may not have their permissions modified.")
            return
        }
        if(pieces.size() == 1) {
            module.mode = AccessMode.RESTRICTED
            bot.send(channel, "Module '${module.name()}' restricted.")
            return
        }
        Command command = module.commands.find { it.name == pieces[1] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            command.mode = AccessMode.RESTRICTED
            bot.send(channel, "Command '${command.name}' of module '${module.name()}' restricted.")
        }
        else {
            bot.send(channel, "You can't modify the permissions of that command.")
        }
    }

    void doDisable(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded.")
            return
        }
        if(module.name() == name()) {
            bot.send(channel, "Module '${name()}' and its commands may not have their permissions modified.")
            return
        }
        if(pieces.size() == 1) {
            module.mode = AccessMode.DISABLED
            bot.send(channel, "Module '${module.name()}' disabled.")
            return
        }
        Command command = module.commands.find { it.name == pieces[1] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            command.mode = AccessMode.DISABLED
            bot.send(channel, "Command '${command.name}' of module '${module.name()}' disabled.")
        }
        else {
            bot.send(channel, "You can't modify the permissions of that command.")
        }
    }

    void doSave(String channel) {
        bot.send(channel, bot.save())
    }

    void doReload(String channel) {
        bot.send(channel, bot.reload())
    }

    void doAddAdmin(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String nick = pieces[0]
        if(bot.isAdmin(nick)) {
            bot.send(channel, "User '${nick}' is already an admin.")
            return
        }
        bot.addAdmin(nick)
        bot.send(channel, "User '${nick}' added as an admin.")
    }

    void doDelAdmin(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        String nick = pieces[0]
        if(!bot.isAdmin(nick)) {
            bot.send(channel, "User '${nick}' is not an admin.")
            return
        }
        bot.removeAdmin(nick)
        bot.send(channel, "User '${nick}' removed as an admin.")
    }

    void doAdmins(String channel) {
        bot.send(channel, "Owner: ${bot.owner()}.${(bot.admins()) ? '\nAdmins: ' + bot.admins().join(', ') + '.' : ''}")
    }

    void doAlias(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded.")
            return
        }
        if(pieces.size() < 2) {
            bot.send(channel, "You need to provide a command to add an alias to.")
            return
        }
        pieces.remove(0)
        Command command = module.commands.find { it.name == pieces[0] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[0]) ?: ''}'.")
        }
        else if(pieces.size() < 2) {
            bot.send(channel, "You need to provide an alias to add.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            pieces.remove(0)
            String alias = pieces.join(' ')
            if(alias in command.triggers) {
                bot.send(channel, "Command '${command.name}' of module '${module.name()}' already aliased to '${alias ?: ''}'.")
                return
            }
            command.triggers << alias
            bot.send(channel, "Added '${alias}' as an alias for command '${command.name}' of module '${module.name()}'.")
        }
        else {
            bot.send(channel, "You can't modify the aliases of that command.")
        }
    }

    void doDeAlias(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded.")
            return
        }
        if(pieces.size() < 2) {
            bot.send(channel, "You need to provide a command to remove an alias from.")
            return
        }
        pieces.remove(0)
        Command command = module.commands.find { it.name == pieces[0] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[0]) ?: ''}'.")
        }
        else if(pieces.size() < 2) {
            bot.send(channel, "You need to provide an alias to remove.")
        }
        pieces.remove(0)
        String alias = pieces.join(' ')
        if(!(alias in command.triggers)) {
            bot.send(channel, "Command '${command.name}' of module '${module.name()}' is not aliased to '${alias ?: ''}'.")
        }
        else if(command.triggers.size() < 2) {
            bot.send(channel, "Cannot remove the last alias of a command.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            String result = command.triggers.remove(alias)
            bot.send(channel, "Removed '${alias ?: ''}' as an alias for command '${command.name}' of module '${module.name()}'.")
        }
        else {
            bot.send(channel, "You can't modify the aliases of that command.")
        }
    }

    void doAliases(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        if(pieces.size() < 2) {
            bot.send(channel, "You need to give both a module and a command.")
            return
        }
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded.")
            return
        }
        Command command = module.commands.find { it.name == pieces[1] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            bot.send(channel, "${command.name} is aliased to: ${command.triggers.join(', ')}.")
        }
    }

    void doHelp(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        if(!pieces) {
            bot.send(sender, getModules())
            return
        }
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(sender, "No module ${pieces[0]} loaded.")
            return
        }
        Command command = module.commands.find { it.name == pieces[1] }
        if(pieces.size() == 1) {
            bot.send(sender, showModuleInfo(module))
        }
        else if(!command) {
            bot.send(sender, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        else {
            bot.send(sender, showCommandInfo(command))
        }
    }

    String getModules() {
        List active = []
        List restricted = []
        List owneronly = []
        List disabled = []
        bot.modules.each { module ->
            if(module.mode == AccessMode.ENABLED) active << module.name()
            else if(module.mode == AccessMode.RESTRICTED) restricted << module.name()
            else if(module.mode == AccessMode.OWNER_ONLY) owneronly << module.name()
            else disabled << module.name()
        }
        String message = "Modules:\n"
        message += active ? "|   Enabled: ${active.join(', ')}\n" : ''
        message += restricted ? "|   Restricted: ${restricted.join(', ')}\n" : ''
        message += owneronly ? "|   Owner-only: ${owneronly.join(', ')}\n" : ''
        message += disabled ? "|   Disabled: ${disabled.join(', ')}\n" : ''
        message = message ?: '|   No modules loaded.'
        return message
    }

    String showModuleInfo(Module module) {
        List active = []
        List restricted = []
        List owneronly = []
        List disabled = []
        module.commands.each { command ->
            if(command.mode == AccessMode.ENABLED) active << command.name
            else if(command.mode == AccessMode.RESTRICTED) restricted << command.name
            else if(command.mode == AccessMode.OWNER_ONLY) owneronly << command.name
            else disabled << command.name
        }
        String message = (module.helpMessage) ? "Module ${module.name()}: ${module.helpMessage}\n" : "${module.name()}:\n"
        message += active ? "|   Enabled: ${active.join(', ')}\n" : ''
        message += restricted ? "|   Restricted: ${restricted.join(', ')}\n" : ''
        message += owneronly ? "|   Owner-only: ${owneronly.join(', ')}\n" : ''
        message += disabled ? "|   Disabled: ${disabled.join(', ')}\n" : ''
        message = message ?: '|   No commands in module \'${module.name()}\'.'
        return message
    }

    String showCommandInfo(Command command) {
        String message = (command.helpMessage) ? "Command ${command.name}: ${command.helpMessage}\n" : "${command.name}:\n"
        AccessMode accessability = command.mode
        Set triggers = command.triggers
        List on = command.on.collect { Action.toString(it) }
        message += "|   Status:       ${accessability}\n"
        message += "|   Triggered by: ${triggers.join(', ')}\n"
        message += "|   Activates on: ${on.join(', ')}\n"
        return message
    }
}