package reshy

import org.jibble.pircbot.*
import reshy.data.Action
import reshy.module.Module
import reshy.module.CoreModule
import reshy.module.DiceModule
import reshy.util.BotAccessData
import reshy.util.BotOptions

class ReshBot extends PircBot {

    List<Module> modules = [new CoreModule(), new DiceModule()]
    private BotOptions options
    private BotAccessData accessData

    ReshBot() {
        options = new BotOptions()
        accessData = new BotAccessData()
        this.setName(options.data.bot.nick)
        this.connect(options.data.bot.server)
        init()
    }

    void init() {
        accessData.setOwner(getOptions().accessdata.owner)
        accessData.setAdmins(getOptions().accessdata.admins as Set)
        this.setVerbose(getOptions().bot.verbose)
        this.setMessageDelay(getOptions().bot.messagedelay as long)
        modules.each { module ->
            module.setup(this)
        }
        getOptions().bot.autojoin.each { channel ->
            this.joinChannel(channel)
        }
    }

    boolean isOwner(String user) {
        return accessData.isOwner(user)
    }

    boolean isAdmin(String user) {
        return accessData.isAdmin(user)
    }

    void addAdmin(String user) {
        accessData.addAdmin(user)
    }

    void removeAdmin(String user) {
        accessData.removeAdmin(user)
    }

    List admins() {
        return accessData.admins as List
    }

    String owner() {
        return accessData.owner
    }

    Map getOptions() {
        return options.data
    }

    String save() {
        Map map = [bot:[verbose: false, messagedelay: getMessageDelay(), server: getServer(), nick: getNick(), autojoin: getOptions().bot.autojoin], accessdata: [owner: accessData.owner, admins: accessData.admins as List]]
        modules.each { module ->
            map.put(module.name(), module.getSettings())
        }
        return options.save(map)
    }

    String reload() {
        String reply = options.reload()
        init()
        return reply
    }

    void onAction(String sender, String login, String hostname, String target, String action) {
        modules.each { module ->
            if(module.activeForUser(sender) && module.registers(Action.ACTION)) {
                module.onAction(sender, login, hostname, target, action)
            }
        }
    }

    void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
        modules.each { module ->
            if(module.activeForUser(sourceNick) && module.registers(Action.INVITE)) {
                module.onInvite(targetNick, sourceNick, sourceLogin, sourceHostname, channel)
            }
        }
    }

    void onJoin(String channel, String sender, String login, String hostname) {
        modules.each { module ->
            if(module.activeForUser(sender) && module.registers(Action.JOIN)) {
                module.onJoin(channel, sender, login, hostname)
            }
        }
    }

    void onMessage(String channel, String sender, String login, String hostname, String message) {
        modules.each { module ->
            if(module.activeForUser(sender) && module.registers(Action.MESSAGE)) {
                module.onMessage(channel, sender, login, hostname, message)
            }
        }
    }

    void onPart(String channel, String sender, String login, String hostname) {
        modules.each { module ->
            if(module.activeForUser(sender) && module.registers(Action.PART)) {
                module.onPart(channel, sender, login, hostname)
            }
        }
    }

    void onPrivateMessage(String sender, String login, String hostname, String message) {
        modules.each { module ->
            if(module.activeForUser(sender) && module.registers(Action.PRIVATEMESSAGE)) {
                module.onPrivateMessage(sender, login, hostname, message)
            }
        }
    }

    void send(String channel, String messages) {
        messages.split('\n').each { message ->
            sendMessage(channel, message)
        }
    }
}