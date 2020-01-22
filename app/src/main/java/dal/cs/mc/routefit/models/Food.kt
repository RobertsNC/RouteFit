package dal.cs.mc.routefit.models

import java.io.Serializable

/**
 * This class is a model for the Food object in our database
 */
class Food : Serializable {
    var id: String? = null
    var name: String? = null
    var calories = 0

    constructor(name: String?, calories: Int) {
        this.name = name
        this.calories = calories
    }

    //Empty constructor for firebase
    constructor()

}