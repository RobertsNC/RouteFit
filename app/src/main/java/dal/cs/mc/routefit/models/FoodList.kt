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
import dal.cs.mc.routefit.R
import dal.cs.mc.routefit.helpers.AddFoodActivity



class FoodList : AppCompatActivity() {

    lateinit var nFoodRecyclerView: RecyclerView
    lateinit var nExerciseRecyclerView: RecyclerView

    lateinit var nDatabase: FirebaseFirestore
    private var adapter: FoodListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_list)

        nDatabase = FirebaseFirestore.getInstance()
        nFoodRecyclerView = findViewById(R.id.foodRecyclerView)
        nFoodRecyclerView.layoutManager =  LinearLayoutManager(this)






        //next two lines needed for back button
        assert(supportActionBar != null)   //null check
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)   //show back button

        val addFoodButton: FloatingActionButton = findViewById<FloatingActionButton>(R.id.addFoodButton)


        addFoodButton.setOnClickListener {
            val intent = Intent(this, AddFoodActivity::class.java)
            startActivity(intent)
        }


        loadInFoodExerciseLists()



        val query: Query = nDatabase.collection("FoodList").orderBy("name")
        val options: FirestoreRecyclerOptions<Food> =
            FirestoreRecyclerOptions.Builder<Food>().setQuery(query, Food::class.java).build()

        adapter = FoodListAdapter(options)
        nFoodRecyclerView.adapter = adapter


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
        itemTouchHelper.attachToRecyclerView(nFoodRecyclerView)
    }

    private fun loadInFoodExerciseLists() {

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

    override fun onSupportNavigateUp(): Boolean { //back button on action bar
        finish()
        return true
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

}





