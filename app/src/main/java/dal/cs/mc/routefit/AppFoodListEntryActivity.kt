package dal.cs.mc.routefit

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import dal.cs.mc.routefit.models.Food

class AppFoodListEntryActivity : AppCompatActivity() {

    lateinit var nDatabase: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_food_list_entry)

        nDatabase = FirebaseFirestore.getInstance()

        val createFoodButton = findViewById<Button>(R.id.createFoodButton)
        val foodEntry = findViewById<EditText>(R.id.enterFoodName)
        val caloriesEntry = findViewById<EditText>(R.id.enterFoodCalories)

        //next two lines needed for back button
        assert(supportActionBar != null)   //null check
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)   //show back button
        
        createFoodButton.setOnClickListener {

            var foodString: String = foodEntry.text.toString()
            var calString: String = caloriesEntry.text.toString()

            if(foodString.isNotEmpty() && calString.isNotEmpty()){

               val documentReference: DocumentReference = nDatabase.collection("AppFoodList").document()

               val food = Food(foodString, Integer.parseInt(calString))

               food.id = documentReference.id
               documentReference.set(food)

                Toast.makeText(
                    this@AppFoodListEntryActivity,
                    "Created Successfully",
                    Toast.LENGTH_SHORT
                ).show()

               finish()
            }

            else{
                Toast.makeText(
                    this@AppFoodListEntryActivity,
                    "There are empty fields. Cannot create new food",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { //back button on action bar
        finish()
        return true
    }
}
