package dal.cs.mc.routefit.helpers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dal.cs.mc.routefit.AppExerciseListEntryActivity
import dal.cs.mc.routefit.R
import dal.cs.mc.routefit.models.Exercise
import kotlinx.android.synthetic.main.activity_add_exercise.*

class AddExerciseActivity : AppCompatActivity() {

    lateinit var nFoodRecyclerView: RecyclerView
    lateinit var nDatabase: FirebaseFirestore
    private var adapter: MainExerciseListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_exercise)

        val enterExerciseButton: FloatingActionButton = findViewById(R.id.enterExerciseButton)


        initBackButton()

        enterExerciseButton.setOnClickListener {
            val intent = Intent(this, AppExerciseListEntryActivity::class.java)
            startActivity(intent)
        }

        nDatabase = FirebaseFirestore.getInstance()
        nFoodRecyclerView = findViewById(R.id.addExerciseRecyclerView)
        nFoodRecyclerView.layoutManager =  LinearLayoutManager(this)


        val query: Query = nDatabase.collection("AppExerciseList").orderBy("name")
        val options: FirestoreRecyclerOptions<Exercise> =
            FirestoreRecyclerOptions.Builder<Exercise>().setQuery(query, Exercise::class.java).build()

        adapter = MainExerciseListAdapter(options)
        nFoodRecyclerView.adapter = adapter

    }

    private fun initBackButton() {
        //next two lines needed for back button
        assert(supportActionBar != null)   //null check
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)   //show back button
    }

    override fun onStart() {
        super.onStart()
        adapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()

        if(adapter != null){
            adapter!!.stopListening()
        }
    }


    private inner class MainExerciseView internal constructor(private val view: View) : RecyclerView.ViewHolder(view){
        internal fun setFields(name: String, calories: Int){
            val exerciseText = view.findViewById<TextView>(R.id.exerciseName)
            exerciseText.text = name

            val caloriesText = view.findViewById<TextView>(R.id.exerciseCalories)
            caloriesText.text = "Calories/Hour: "+calories.toString()
        }

        internal fun setAddButton(name: String, caloriesPerHour: Int){

            val addExercise = view.findViewById<Button>(R.id.addFood)
            addExercise.setOnClickListener {

                /**
                 * Inflate popup window with a spinner from which the user can select the
                 * amount of time for which they have exercised. We use this time multiplied with
                 * the calories per hour field to determine calories burned
                 */
                val inflater: LayoutInflater =
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popView = inflater.inflate(R.layout.popup_add_exercise, null)

                val window = PopupWindow(
                    popView,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                window.elevation = 10.0F

                window.isFocusable = true


                val popupSpinner = popView.findViewById<Spinner>(R.id.exerciseLengthSpinner)


                popupSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{

                    //We use this count to prevent the selection listener from firing without the user making a selection
                    var count = 0
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                        if(count >= 1){

                            var time: Double = java.lang.Double.parseDouble(parent!!.getItemAtPosition(position).toString())

                            /**
                             * Create item in the user's exercise list and finish
                             */
                            var exercise = Exercise(name, caloriesPerHour)
                            val documentReference: DocumentReference = nDatabase.collection("ExerciseList").document()
                            exercise.id = documentReference.id
                            exercise.time = time
                            documentReference.set(exercise)
                            finish()
                        }
                        count++
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {

                    }
                }

                //show window
                TransitionManager.beginDelayedTransition(root_layout)
                window.showAtLocation(
                    root_layout,
                    Gravity.CENTER,
                    0,
                    0
                )
            }
        }
    }


    private inner class MainExerciseListAdapter internal constructor(options: FirestoreRecyclerOptions<Exercise>) :
        FirestoreRecyclerAdapter<Exercise, MainExerciseView>(options){

        override fun onBindViewHolder(holder: MainExerciseView, position: Int, model: Exercise) {
            holder.setFields(model.name!!, model.caloriesPerHour)
            holder.setAddButton(model.name!!, model.caloriesPerHour)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainExerciseView {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.add_exercise_card, parent, false)
            return MainExerciseView(view)
        }
    }
    override fun onSupportNavigateUp(): Boolean { //back button on action bar
        finish()
        return true
    }
}
