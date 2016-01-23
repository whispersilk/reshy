package reshy.data

enum AccessMode {

    ENABLED('enabled'), RESTRICTED('restricted'), OWNER_ONLY('owneronly'), DISABLED('disabled')

    String stringValue

    AccessMode(stringValue) {
        this.stringValue = stringValue
    }

    static String toString(AccessMode mode) {
        return mode.stringValue
    }

    static AccessMode fromString(String name) {
        return AccessMode.values().find { it.stringValue == name }
    }
}