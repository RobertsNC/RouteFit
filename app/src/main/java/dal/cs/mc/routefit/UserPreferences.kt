package dal.cs.mc.routefit

/**
 * This class is a singleton that holds user preferences that
 * can be passed around to where they're needed
 */
class UserPreferences{
    companion object{
        val instance = UserPreferences()
        var status: Int = 0
        var preference: String = "workout"
        var foodNotification: Int = 0
    }
}