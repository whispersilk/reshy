package reshy.data

enum Action {
     ACTION('action'), INVITE('invite'), JOIN('join'), MESSAGE('message'), PART('part'), PRIVATEMESSAGE('pm')

     String stringValue

     Action(stringValue) {
        this.stringValue = stringValue
     }

     static String toString(Action action) {
        return action.stringValue
     }

     static Action fromString(String string) {
        return Action.values().find { it.stringValue == string }
     }
}