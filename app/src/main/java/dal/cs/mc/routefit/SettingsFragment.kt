package dal.cs.mc.routefit

import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore


class SettingsFragment : PreferenceFragmentCompat() {

    lateinit var nDatabase: FirebaseFirestore


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

    }

    override fun onPause() {
        super.onPause()
        updateUserPreferences()
    }

    private fun updateUserPreferences() {
        val userMapVisibilitySetting =  getMapVisibilitySetting()

        if (userMapVisibilitySetting)
            UserPreferences.status = 0
        else
            UserPreferences.status = 1

        val exerciseTypeSetting = getExerciseType()
        UserPreferences.preference = exerciseTypeSetting

        val foodNotificationSetting = getFoodNotificationSetting()

        if(foodNotificationSetting)
            UserPreferences.foodNotification = 0
        else
            UserPreferences.foodNotification = 1


    }

    private fun getFoodNotificationSetting(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context/* Activity context */)
        val foodNotificationSetting = sharedPreferences.getBoolean("food_notification_setting", false)
        Log.d("SettingsFragment", "food notification setting: $foodNotificationSetting")
        return foodNotificationSetting
    }

    private fun getMapVisibilitySetting(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context/* Activity context */)
        val userMapVisibilitySetting = sharedPreferences.getBoolean("map_visibility_setting", false)
        Log.d("SettingsFragment", "user map vis setting:"+userMapVisibilitySetting.toString())
        return userMapVisibilitySetting
    }

    private fun getExerciseType(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context/* Activity context */)
        val exerciseTypeSetting = sharedPreferences.getString("exercise_type_setting", "workout")
        Log.d("SettingsFragment", "exercise type setting: "+exerciseTypeSetting.toString())
        return exerciseTypeSetting.toString()
    }

}
