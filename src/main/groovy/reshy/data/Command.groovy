package reshy.data

class Command {
    
    String name         // Name of the command.
    AccessMode mode     // Restriction mode.
    Set on              // The Actions that can trigger this command.
    Set triggers        // What keys trigger the command.
    Closure condition   // The condition used to determine if the command should be triggered.
    Closure action      // The action to take when the command is triggered.
    String helpMessage  // The help message to be shown if information is requested.
    String errMessage   // The message to send if the command is not allowed to be sent (user doesn't meet requirements).

    boolean hasTrigger(String trigger) {
        return (trigger in triggers)
    }
}