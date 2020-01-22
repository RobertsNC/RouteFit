package dal.cs.mc.routefit.models

import com.google.firebase.firestore.GeoPoint
import java.io.Serializable

/**
 * This class is a model for the User object in firebase
 */

class User : Serializable{
    var id: String? = null
    var name: String? = null
    var status: Int = 0
    var preference: String? = null
    var location: GeoPoint? = null

    constructor(name: String?, status: Int, preference: String?, location: GeoPoint?){
        this.name = name
        this.status = status
        this.preference = preference
        this.location = location
    }

    //empty constructor for firebase
    constructor()

}