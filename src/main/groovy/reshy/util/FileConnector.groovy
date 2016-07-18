package reshy.util

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class FileConnector {

    private File file;

    FileConnector(String dir, String name) {
        file = new File(dir, name)
        file.createNewFile()
    }

    FileConnector(File fileObj) {
        file = fileObj
        file.createNewFile()
    }

    boolean save(def o) {
        if(!(o instanceof Map) && !(o instanceof List)) {
            return false
        }
        file.delete()
        file.createNewFile()
        file.withWriter { writer ->
            writer.write JsonOutput.prettyPrint(JsonOutput.toJson(o))
        }
        return true
    }

    def load(String nullVal = 'null') {
        String jsonString = file.collect { it }.join(' ')
        if(!jsonString) {
            if (nullVal == 'map') { return [:] }
            else if (nullVal == 'list') { return [] }
            else { return null }
        }
        return new JsonSlurper().parseText(jsonString)
    }
}