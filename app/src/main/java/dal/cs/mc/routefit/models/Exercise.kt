package dal.cs.mc.routefit.models

import java.io.Serializable

/**
 * This class is a model for the Exercise object in our database
 */
class Exercise : Serializable {

    var id: String? = null
    var name: String? = null
    var time: Double = 0.0
    var caloriesPerHour: Int = 0
    var calories: Int = 0

    constructor(name: String?,  caloriesPerHour: Int) {
        this.name = name
        this.caloriesPerHour = caloriesPerHour
    }

    //empty constructor for firebase
    constructor()

}