package dal.cs.mc.routefit.models

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dal.cs.mc.routefit.HomeActivity
import dal.cs.mc.routefit.R
import dal.cs.mc.routefit.helpers.AddExerciseActivity

class ExerciseList : AppCompatActivity() {

    lateinit var nExerciseRecyclerView: RecyclerView
    lateinit var nDatabase: FirebaseFirestore
    private var adapter: ExerciseListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)


        //next two lines needed for back button
        assert(supportActionBar != null)   //null check
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)   //show back button

        val addExerciseButton = findViewById<FloatingActionButton>(R.id.addExerciseButton)
        addExerciseButton.setOnClickListener {
            val intent = Intent(this, AddExerciseActivity::class.java)
            startActivity(intent)
        }

        nDatabase = FirebaseFirestore.getInstance()
        nExerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)
        nExerciseRecyclerView.layoutManager =  LinearLayoutManager(this)


        val query: Query = nDatabase.collection("ExerciseList").orderBy("name")
        val options: FirestoreRecyclerOptions<Exercise> =
            FirestoreRecyclerOptions.Builder<Exercise>().setQuery(query, Exercise::class.java).build()

        adapter = ExerciseListAdapter(options)
        nExerciseRecyclerView.adapter = adapter


        /**
         * This callback allows us to attach a helper to the recyclerview
         * that allows us to implement delete on swipe to the left or right
         */
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                  direction: Int) {
                adapter!!.deleteItem(viewHolder.adapterPosition)
            }
        }


        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(nExerciseRecyclerView)


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

    override fun onSupportNavigateUp(): Boolean {//back button on action bar
        finish()
        return true
    }


    private inner class ExerciseView internal constructor(private val view: View) : RecyclerView.ViewHolder(view){
        internal fun setFields(name: String, calories: Int){
            val exerciseText = view.findViewById<TextView>(R.id.exerciseName)
            exerciseText.text = name

            val caloriesText = view.findViewById<TextView>(R.id.exerciseCalories)
            caloriesText.text = "Calories/Hour: "+calories.toString()
        }


    }


    private inner class ExerciseListAdapter internal constructor(options: FirestoreRecyclerOptions<Exercise>) :
        FirestoreRecyclerAdapter<Exercise, ExerciseView>(options){

        override fun onBindViewHolder(holder: ExerciseView, position: Int, model: Exercise) {
            holder.setFields(model.name!!, model.caloriesPerHour)


        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseView {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.exercise_card, parent, false)
            return ExerciseView(view)
        }

        fun deleteItem(position: Int) {
            snapshots.getSnapshot(position).reference.delete()
        }
    }

}
