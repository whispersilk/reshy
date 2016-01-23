package reshy.module

import reshy.ReshBot
import reshy.data.Action
import reshy.data.AccessMode
import reshy.data.Command

abstract class Module {

    Map options
    ReshBot bot
    AccessMode mode
    List<Command> commands

    Module() {
        initCommands()
        commands.each { command -> command.condition?.delegate = command }
    }

    boolean activeForUser(String user) {
        if(mode == AccessMode.ENABLED || (mode == AccessMode.RESTRICTED && bot.isAdmin(user)) || (mode == AccessMode.OWNER_ONLY && bot.isOwner(user))) {
            return true
        }
        return false
    }

    boolean isValid(Command command, String user) {
        if(command.mode == AccessMode.ENABLED || (command.mode == AccessMode.RESTRICTED && bot.isAdmin(user)) || (command.mode == AccessMode.OWNER_ONLY && bot.isOwner(user))) {
            return true
        }
        return false
    }

    abstract void initCommands()

    abstract String name()
    
    abstract boolean registers(Action action)

    abstract void setup(ReshBot bot)

    abstract Map getSettings()

    void onAction(String sender, String login, String hostname, String target, String action) {
        return
    }

    void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
        return
    }

    void onJoin(String channel, String sender, String login, String hostname) {
        return
    }

    void onMessage(String channel, String sender, String login, String hostname, String message) {
        return
    }

    void onPart(String channel, String sender, String login, String hostname) {
        return
    }

    void onPrivateMessage(String sender, String login, String hostname, String message) {
        return
    }
}