package reshy.data

class Tell {
    String sender
    String recipient
    String message

    String print() {
        return "${sender} wanted me to tell you \"${message}\""
    }
}