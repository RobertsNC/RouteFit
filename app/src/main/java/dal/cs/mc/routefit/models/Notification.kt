package dal.cs.mc.routefit.models

import java.io.Serializable
import java.util.Date

/**
 * This class is a model for the Food object in our database
 */
class Notification : Serializable {
    var id: String? = null
    var title: String? = null
    var content: String? = null


    constructor(title: String?, content: String?) {
        this.title = title
        this.content = content
    }

    //Empty constructor for firebase
    constructor()

}
