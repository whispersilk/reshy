package reshy.util

class BotAccessData {
    String owner
    Set admins

    boolean isOwner(String user) {
        return owner == user
    }
    boolean isAdmin(String user) {
        return (isOwner(user) || user in admins)
    }

    void addAdmin(String user) {
        admins.add(user)
    }

    void removeAdmin(String user) {
        admins.removeElement(user)
    }
}