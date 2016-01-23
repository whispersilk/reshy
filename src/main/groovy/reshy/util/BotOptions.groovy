package reshy.util

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class BotOptions {

    Map data
    private static final String DEFAULT_PATH_TO_FILE = System.getProperty('user.dir')
    private static final String DEFAULT_FILE_NAME = 'config.json'
    String path
    String fileName

    BotOptions() {
        this.path = DEFAULT_PATH_TO_FILE
        this.fileName = DEFAULT_FILE_NAME
        reload()    
    }

    BotOptions(String fileName) {
        this.path = DEFAULT_PATH_TO_FILE
        this.fileName = fileName
        reload()
    }

    BotOptions(String path, String fileName) {
        this.path = path
        this.fileName = fileName
        reload()
    }

    String reload() {
        File file = new File(path, fileName)
        String dataString = file.collect { it }.join(' ')
        data = new JsonSlurper().parseText(dataString)
        return 'Options file reloaded.'
    }

    String save(Map toSave) {
        File file = new File(path, fileName)
        file.delete()
        file.createNewFile()
        file.withWriter { writer ->
            writer.write JsonOutput.prettyPrint(JsonOutput.toJson(toSave))
        }
        return 'Current settings saved.'
    }
}