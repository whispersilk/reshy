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

    private static final List REGISTERS = [Action.MESSAGE, Action.PRIVATEMESSAGE]

    void initCommands() {
        commands = [
            [name: 'join', mode: AccessMode.RESTRICTED, triggers: ['~join'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doJoin(msc[0]) }
            ] as Command,
            [name: 'leave', mode: AccessMode.RESTRICTED, triggers: ['~leave'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc ->  doLeave(msc[0]) }
            ] as Command,
            [name: 'enable', mode: AccessMode.RESTRICTED, triggers: ['~enable'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doEnable(*msc) }
            ] as Command,
            [name: 'restrict', mode: AccessMode.RESTRICTED, triggers: ['~restrict'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doRestrict(*msc)}
            ] as Command,
            [name: 'disable', mode: AccessMode.RESTRICTED, triggers: ['~disable'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doDisable(*msc)}
            ] as Command,
            [name: 'save', mode: AccessMode.RESTRICTED, triggers: ['~save'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doSave(msc[2])}
            ] as Command,
            [name: 'reload', mode: AccessMode.RESTRICTED, triggers: ['~reload'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doReload(msc[2])}
            ] as Command,
            [name: 'addadmin', mode: AccessMode.OWNER_ONLY, triggers: ['~addadmin'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAddAdmin(*msc)}
            ] as Command,
            [name: 'deladmin', mode: AccessMode.OWNER_ONLY, triggers: ['~deladmin'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doDelAdmin(*msc)}
            ] as Command,
            [name: 'admins', mode: AccessMode.RESTRICTED, triggers: ['~admins'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAdmins(msc[2]) }
            ] as Command,
            [name: 'alias', mode: AccessMode.RESTRICTED, triggers: ['~alias'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAlias(*msc) }
            ] as Command,
            [name: 'dealias', mode: AccessMode.RESTRICTED, triggers: ['~dealias'], on: [Action.MESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doDeAlias(*msc) }
            ] as Command,
            [name: 'aliases', mode: AccessMode.RESTRICTED, triggers: ['~aliases'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> doAliases(*msc) }
            ] as Command,
            [name: 'commands', mode: AccessMode.RESTRICTED, triggers: ['~commands'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> showCommands(*msc) }
            ] as Command,
            [name: 'modules', mode: AccessMode.RESTRICTED, triggers: ['~modules'], on: [Action.MESSAGE, Action.PRIVATEMESSAGE],
                condition: { String message -> delegate.hasTrigger(message.split(' ')[0]) },
                action: { String... msc -> showModules(msc[2]) }
            ] as Command
        ]
    }

    String name() {
        return 'core'
    }

    boolean registers(Action action) {
        return (action in REGISTERS)
    }

    void setup(ReshBot bot) {
        this.bot = bot
        this.options = bot.getOptions().core
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
        Command command = module.commands.find { it.name == pieces[1] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        else if(pieces.size() < 3) {
            bot.send(channel, "You need to provide an alias to add.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            if(pieces[2] in command.triggers) {
                bot.send(channel, "Command '${command.name}' of module '${module.name()}' already aliased to '${(pieces[2]) ?: ''}'.")
                return
            }
            command.triggers << pieces[2]
            bot.send(channel, "Added '${pieces[2]}' as an alias for command '${command.name}' of module '${module.name()}'.")
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
        Command command = module.commands.find { it.name == pieces[1] }
        if(!command) {
            bot.send(channel, "Module '${module.name()}' doesn't have a command '${(pieces[1]) ?: ''}'.")
        }
        else if(pieces.size() < 3) {
            bot.send(channel, "You need to provide an alias to remove.")
        }
        if(!(pieces[2] in command.triggers)) {
            bot.send(channel, "Command '${command.name}' of module '${module.name()}' is not aliased to '${(pieces[2]) ?: ''}'.")
        }
        else if(command.triggers.size() < 2) {
            bot.send(channel, "Cannot remove the last alias of a command.")
        }
        else if(bot.isOwner(sender) || (command.mode != AccessMode.OWNER_ONLY)) {
            String result = command.triggers.remove(pieces[2])
            bot.send(channel, "Removed '${(pieces[2]) ?: ''}' as an alias for command '${command.name}' of module '${module.name()}'.")
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

    void showCommands(String message, String sender, String channel) {
        List pieces = message.split(' ')
        pieces.remove(0)
        Module module = bot.modules.find { it.name() == pieces[0] }
        if(!module) {
            bot.send(channel, "No module '${(pieces[0]) ?: ''}' is loaded. To get a list of loaded modules, try the modules command.")
            return
        }
        List l = []
        module.commands.each { command ->
            if(isValid(command, sender) || command.mode == AccessMode.DISABLED) {
                l << command.name
            }
        }
        bot.send(channel, "Commands of module '${module.name()}': ${l.join(', ')}")
    }

    void showModules(String channel) {
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
        String message = ''
        message += active ? "Enabled: ${active.join(', ')}\n" : ''
        message += restricted ? "Restricted: ${restricted.join(', ')}\n" : ''
        message += owneronly ? "Owner-only: ${owneronly.join(', ')}\n" : ''
        message += disabled ? "Disabled: ${disabled.join(', ')}\n" : ''
        message = message ?: 'No modules loaded.'
        bot.send(channel, message)
    }
}