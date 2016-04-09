package reshy.util

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class FileConnector {

    private String fileDir;
    private String fileName;
    private File file;

    FileConnector(String dir, String name) {
        this.fileDir = dir;
        this.fileName = name;
        file = new File(fileDir, fileName)
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