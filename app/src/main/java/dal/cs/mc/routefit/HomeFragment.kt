package dal.cs.mc.routefit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dal.cs.mc.routefit.helpers.AddExerciseActivity
import dal.cs.mc.routefit.helpers.AddFoodActivity
import dal.cs.mc.routefit.models.Exercise
import dal.cs.mc.routefit.models.Food

class HomeFragment : Fragment() {

    lateinit var nFoodRecyclerView: RecyclerView
    lateinit var nExerciseRecyclerView: RecyclerView

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var nDatabase: FirebaseFirestore
    private lateinit var food: Food
    private lateinit var exercise: Exercise

    private var foodListAdapter: HomeFragment.FoodListAdapter? = null
    private var exerciseListAdapter: HomeFragment.ExerciseListAdapter? = null

    //need these to access the calories aggregations outside of the on success listener
    object GlobalCalCounts{
        var exerciseCals = 0.0
        var foodCals = 0
    }

    // "Main" function. Sets the view, displays the current calorie counts
    // listens for food and exercise button clicks to take user to the respective activity.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = setView(inflater, container, savedInstanceState)
        populateFoodExerciseLists(root)
        handleFoodSwipeDelete()
        handleExerciseSwipeDelete()
        displayCalories(root.findViewById(R.id.caloriesConsumed), root.findViewById(R.id.caloriesBurned), root.findViewById(R.id.caloriesTotal))//display food cal, expended cal, and total cal
        listenForFoodButtonPress(root)//listens for food button click to take user to food activity
        listenForExerciseButtonPress(root)//listens for exercise button click to take user to exercise activity
        


        return root
    }

    private fun handleExerciseSwipeDelete() {
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
                exerciseListAdapter!!.deleteItem(viewHolder.adapterPosition)
            }
        }


        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(nExerciseRecyclerView)
    }

    private fun handleFoodSwipeDelete() {
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
                foodListAdapter!!.deleteItem(viewHolder.adapterPosition)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(nFoodRecyclerView)
    }

    private fun populateFoodExerciseLists(root: View) {
        nDatabase = FirebaseFirestore.getInstance()//init the database
        populateFoodList(root)
        populateExerciseList(root)
    }

    private fun populateExerciseList(root: View) {
        nExerciseRecyclerView = root.findViewById(R.id.exercise_list_RecyclerView_home)
        nExerciseRecyclerView.layoutManager = LinearLayoutManager(root.context)

        val exerciseQuery: Query = nDatabase.collection("ExerciseList").orderBy("name")
        val exerciseOptions: FirestoreRecyclerOptions<Exercise> =
            FirestoreRecyclerOptions.Builder<Exercise>().setQuery(exerciseQuery, Exercise::class.java).build()

        exerciseListAdapter = ExerciseListAdapter(exerciseOptions)
        nExerciseRecyclerView.adapter = exerciseListAdapter
    }

    private fun populateFoodList(root: View) {
        nFoodRecyclerView = root.findViewById(R.id.food_list_RecyclerView_home)
    nFoodRecyclerView.layoutManager =  LinearLayoutManager(root.context)
    val foodQuery: Query = nDatabase.collection("FoodList").orderBy("name")
    val foodOptions: FirestoreRecyclerOptions<Food> =
        FirestoreRecyclerOptions.Builder<Food>().setQuery(foodQuery, Food::class.java).build()

    foodListAdapter = FoodListAdapter(foodOptions)
    nFoodRecyclerView.adapter = foodListAdapter
}

    private fun setView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    private fun listenForExerciseButtonPress(root: View) {
        val exerciseListButton: Button = root.findViewById(R.id.addExercise)!!
        exerciseListButton.setOnClickListener {
            val intent = Intent(getActivity(), AddExerciseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun listenForFoodButtonPress(root: View) {
        val foodListButton : Button = root.findViewById(R.id.addFood)!!
        foodListButton.setOnClickListener {
            val intent = Intent(getActivity(), AddFoodActivity::class.java)
            startActivity(intent)
        }
    }

    private fun displayCalories(
        calDisplay: TextView,
        exerciseDisplay: TextView,
        totCalDisplay: TextView
    ) {

        nDatabase.collection("FoodList").get()
            .addOnSuccessListener { result ->
                var foodCalories: Int = 0
                for(doc in result) {
                    food = doc.toObject(Food::class.java)
                    foodCalories += food.calories

                }
                //set global var and display
                GlobalCalCounts.foodCals = foodCalories
                calDisplay.text = "Consumed: " + foodCalories
            }
            .addOnFailureListener { exception ->
                Log.d("Debug", "", exception)
            }

        nDatabase.collection("ExerciseList").get()
            .addOnSuccessListener{ result ->
                var exCalories = 0.0
                for(doc in result){
                    exercise = doc.toObject(Exercise::class.java)
                    //This is a run, this needs to be parsed differently
                    if(exercise.calories != 0){
                        exCalories +=  exercise.calories!!
                    }
                    //otherwise parse as normal. This is a normally entered exercise
                    else{
                        exCalories += (exercise.caloriesPerHour*exercise.time)
                    }

                }
                //set global var and display
                GlobalCalCounts.exerciseCals = exCalories
                exerciseDisplay.text = "Burned: " + exCalories
            }

        //set total display
        totCalDisplay.text = "Total: " + (GlobalCalCounts.foodCals.toDouble() - GlobalCalCounts.exerciseCals)
    }


    override fun onStart() {
        super.onStart()
        foodListAdapter!!.startListening()
        exerciseListAdapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()

        if(foodListAdapter != null){
            foodListAdapter!!.stopListening()
        }
        if(exerciseListAdapter != null){
            exerciseListAdapter!!.stopListening()
        }
    }



    private inner class FoodView internal constructor(private val view: View) : RecyclerView.ViewHolder(view){
        internal fun setFields(name: String, calories: Int){
            val foodText = view.findViewById<TextView>(R.id.foodName)
            foodText.text = name

            val caloriesText = view.findViewById<TextView>(R.id.foodCalories)
            caloriesText.text = "Calories: "+calories.toString()
        }


    }

    private inner class FoodListAdapter internal constructor(options: FirestoreRecyclerOptions<Food>) :
        FirestoreRecyclerAdapter<Food, FoodView>(options){

        override fun onBindViewHolder(holder: FoodView, position: Int, model: Food) {
            holder.setFields(model.name!!, model.calories)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodView {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.food_card, parent, false)
            return FoodView(view)
        }

        fun deleteItem(position: Int) {
            snapshots.getSnapshot(position).reference.delete()
        }
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
