package reshy.data

class Seen {
    String user
    String channel
    String message
    Calendar time

    static Seen fromMap(Map m) {
        Calendar cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, m.year)
        cal.set(Calendar.MONTH, m.month)
        cal.set(Calendar.DAY_OF_MONTH, m.date)
        cal.set(Calendar.HOUR_OF_DAY, m.hour)
        cal.set(Calendar.MINUTE, m.minute)
        return [user: m.user, channel: m.channel, message: m.message, time: cal] as Seen
    }

    static Map toMap(Seen s) {
        return [user: s.user, channel: s.channel, message: s.message, year: s.time.get(Calendar.YEAR), month: s.time.get(Calendar.MONTH), date: s.time.get(Calendar.DAY_OF_MONTH), hour: s.time.get(Calendar.HOUR_OF_DAY), minute: s.time.get(Calendar.MINUTE)]
    }

    String print() {
        String timeSince = since(time)
        return "I last saw $user $timeSince ago, saying \"$message\" on channel ${channel}."
    }

    private String since(Calendar cal) {
        int number = 1
        Calendar now = Calendar.getInstance()
        Calendar clone = (Calendar)cal.clone()
        int years = elapsed(clone, now, Calendar.YEAR)
        clone.add(Calendar.YEAR, years)
        int months = elapsed(clone, now, Calendar.MONTH)
        clone.add(Calendar.MONTH, months)
        int days = elapsed(clone, now, Calendar.DATE)
        clone.add(Calendar.DATE, days)
        int hours = ((now.getTimeInMillis() - clone.getTimeInMillis()) / 3600000) as int
        clone.add(Calendar.HOUR_OF_DAY, hours)
        int minutes = ((now.getTimeInMillis() - clone.getTimeInMillis()) / 60000) as int
        List result = []
        if(years) result << "$years year${(years > 1) ? 's' : ''}"
        if(months) result << "$months month${(months > 1) ? 's' : ''}"
        if(days) result << "$days day${(days > 1) ? 's' : ''}"
        if(hours) result << "$hours hour${(hours > 1) ? 's' : ''}"
        if(minutes || result.size() == 0) result << "$minutes minute${(minutes != 1) ? 's' : ''}"
        return result.join(', ')
    }

    private int elapsed(Calendar notRecent, Calendar recent, int field) {
        Calendar old = (Calendar) notRecent.clone()
        int elapsed = -1
        while(!old.after(recent)) {
            old.add(field, 1)
            elapsed++
        }
        return elapsed
    }
}