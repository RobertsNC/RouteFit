package dal.cs.mc.routefit

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import dal.cs.mc.routefit.models.Exercise

class AppExerciseListEntryActivity : AppCompatActivity() {

    lateinit var nDatabase: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_exercise_list_entry)

        nDatabase = FirebaseFirestore.getInstance()

        val createExerciseButton = findViewById<Button>(R.id.createExerciseButton)
        val exerciseEntry = findViewById<EditText>(R.id.enterExerciseName)
        val caloriesEntry = findViewById<EditText>(R.id.enterExerciseCalories)

        initBackButton()

        createExerciseButton.setOnClickListener {

            var exerciseString: String = exerciseEntry.text.toString()
            var calString: String = caloriesEntry.text.toString()

            if(exerciseString.isNotEmpty() && calString.isNotEmpty()){

                val documentReference: DocumentReference = nDatabase.collection("AppExerciseList").document()

                val ex = Exercise(exerciseString, Integer.parseInt(calString))

                ex.id = documentReference.id
                documentReference.set(ex)

                Toast.makeText(
                    this@AppExerciseListEntryActivity,
                    "Created Successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }

            else{
                Toast.makeText(
                    this@AppExerciseListEntryActivity,
                    "There are empty fields. Cannot create new food",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    private fun initBackButton() {
        //next two lines needed for back button
        assert(supportActionBar != null)   //null check
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)   //show back button

    }

    override fun onSupportNavigateUp(): Boolean { //back button on action bar
        finish()
        return true
    }
}
