package reshy

import org.jibble.pircbot.*
import reshy.data.Action

import reshy.module.Module
import reshy.module.CapeModule
import reshy.module.CoreModule
import reshy.module.DiceModule
import reshy.module.QuoteModule
import reshy.module.ReactionModule
import reshy.module.SeenTellModule

import reshy.util.BotAccessData
import reshy.util.BotOptions

import com.google.common.base.Splitter

class ReshBot extends PircBot {

    private static final List<Class> MODULE_NAMES = [CoreModule, CapeModule, DiceModule, QuoteModule, ReactionModule, SeenTellModule] // List of module classes to invoke. Groovy allows us to leave off the ".class"
    List<Module> modules
    private BotOptions options
    private BotAccessData accessData

    ReshBot() {
        options = new BotOptions()
        accessData = new BotAccessData()
        this.setName(getOptions().bot.nick)
        this.connect(getOptions().bot.server)
        init()
    }

    void init() {
        modules = []
        accessData.setOwner(getOptions().accessdata.owner)
        accessData.setAdmins(getOptions().accessdata.admins as Set)
        this.setVerbose(getOptions().bot.verbose)
        this.setMessageDelay(getOptions().bot.messagedelay as long)
        this.changeNick(getOptions().bot.nick)
        this.identify(getOptions().bot.password)
        getOptions().bot.autojoin.each { channel ->
            this.joinChannel(channel)
        }
        MODULE_NAMES.each { moduleName ->
            Module module = moduleName.newInstance()
            module.setup(this)
            modules << module
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
        Map map = [bot:[verbose: false, messagedelay: getMessageDelay(), server: getServer(), nick: getNick(), password: getOptions().bot.password, autojoin: getOptions().bot.autojoin], accessdata: [owner: accessData.owner, admins: accessData.admins as List]]
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

    void onDisconnect() {
        init()
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

    // Convenience method for sending multi-line messages.
    void send(String channel, String messages) {
        messages.split('\n').each { message ->
            for(String msg : Splitter.fixedLength(441).split(message)) {
                sendMessage(channel, msg)
            }
        }
    }
}