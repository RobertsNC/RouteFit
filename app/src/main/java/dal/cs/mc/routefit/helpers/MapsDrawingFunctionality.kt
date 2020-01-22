package dal.cs.mc.routefit.helpers

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

//create a BottomSheet class which takes in the data from the Google URL
//and parses it into a Class which we can effectively access
val CALORIES_PER_KM = 30

class RouteInformation {
    var calories: String = ""
    var caloriesInt: Int = 0
    private var distance: Int = 0
    private var destination: String = ""
    var address: String = ""
    var city: String = ""
    var postalCode: String = ""
    var timetaken: String = ""
    var timeMin: Int = 0
    var distanceString: String = ""
    internal var instructions: ArrayList<String> = ArrayList()
    internal var checkpoints: ArrayList<LatLng> = ArrayList()

    fun refactorData(data: JSONArray) {
        distance = data.getJSONObject(0).getJSONObject("distance").getInt("value")
        distanceString = data.getJSONObject(0).getJSONObject("distance").getString("text")
        caloriesInt = caloriesBurned(distance)
        calories = "Calories: $caloriesInt cal"
        destination = data.getJSONObject(0).getString("end_address")
        timetaken = data.getJSONObject(0).getJSONObject("duration").getString("text")
        timeMin= data.getJSONObject(0).getJSONObject("duration").getInt("value")
        timeMin /= 60
        timetaken = "$timetaken, $distanceString"
        address = destination.split(',')[0]
        city = destination.split(',')[1]
        postalCode = destination.split(',')[2]
    }

    fun setInstructions(list: MutableList<String>) {
        instructions.clear()
        for (l in list)
            instructions.add(android.text.Html.fromHtml(l).toString())
    }

    fun caloriesBurned(distance: Int): Int {
        return (distance * CALORIES_PER_KM) / 1000
    }

    fun setCheckpoints(list: ArrayList<LatLng>) {
        checkpoints.clear()
        for (pnt in list) {
            checkpoints.add(pnt)
        }
    }

}

open class MapsDrawingFunctionality {

    fun getStringInstructions(response: String): JSONArray {
        val jsonResponse = JSONObject(response)
        val routes = jsonResponse.getJSONArray("routes")
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        return legs
    }


}