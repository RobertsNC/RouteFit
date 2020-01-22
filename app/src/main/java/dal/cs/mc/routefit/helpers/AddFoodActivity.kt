package dal.cs.mc.routefit.helpers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dal.cs.mc.routefit.AppFoodListEntryActivity
import dal.cs.mc.routefit.R
import dal.cs.mc.routefit.models.Food

class AddFoodActivity : AppCompatActivity() {

    lateinit var nFoodRecyclerView: RecyclerView
    lateinit var nDatabase: FirebaseFirestore
    private var adapter: MainFoodListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        val enterFoodButton: FloatingActionButton = findViewById(R.id.enterFoodButton)

        initBackButton()

        enterFoodButton.setOnClickListener {
            val intent = Intent(this, AppFoodListEntryActivity::class.java)
            startActivity(intent)
        }

        nDatabase = FirebaseFirestore.getInstance()
        nFoodRecyclerView = findViewById(R.id.addFoodRecyclerView)
        nFoodRecyclerView.layoutManager =  LinearLayoutManager(this)


        val query: Query = nDatabase.collection("AppFoodList").orderBy("name")
        val options: FirestoreRecyclerOptions<Food> =
            FirestoreRecyclerOptions.Builder<Food>().setQuery(query, Food::class.java).build()

        adapter = MainFoodListAdapter(options)
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


    private inner class MainFoodView internal constructor(private val view: View) : RecyclerView.ViewHolder(view){
        internal fun setFields(name: String, calories: Int){
            val foodText = view.findViewById<TextView>(R.id.foodName)
            foodText.text = name

            val caloriesText = view.findViewById<TextView>(R.id.foodCalories)
            caloriesText.text = "Calories: "+calories.toString()
        }

        internal fun setAddButton(name: String, calories: Int){
            val addFood = view.findViewById<Button>(R.id.addFood)
            addFood.setOnClickListener {

                var food: Food = Food(name, calories)
                val documentReference: DocumentReference = nDatabase.collection("FoodList").document()
                food.id = documentReference.id

                documentReference.set(food)
                finish()
            }
        }
    }


    private inner class MainFoodListAdapter internal constructor(options: FirestoreRecyclerOptions<Food>) :
        FirestoreRecyclerAdapter<Food, MainFoodView>(options){

        override fun onBindViewHolder(holder: MainFoodView, position: Int, model: Food) {
            holder.setFields(model.name!!, model.calories)
            holder.setAddButton(model.name!!, model.calories)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainFoodView {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.add_food_card, parent, false)
            return MainFoodView(view)
        }
    }
    override fun onSupportNavigateUp(): Boolean { //back button on action bar
        finish()
        return true
    }
}
