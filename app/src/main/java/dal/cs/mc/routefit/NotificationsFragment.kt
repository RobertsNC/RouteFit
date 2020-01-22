package dal.cs.mc.routefit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dal.cs.mc.routefit.models.Notification
import java.util.*


class NotificationsFragment: Fragment() {


    private lateinit var notificationsViewModel: NotificationsViewModel
    lateinit var nNotificationRecyclerView: RecyclerView
    private lateinit var nDatabase: FirebaseFirestore
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
            ViewModelProviders.of(this).get(NotificationsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_notifications, container, false)

        populateNotificationList(root)
        handleSwipe()


        return root
    }

    override fun onStart() {
        super.onStart()
        notificationAdapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()

        if(notificationAdapter != null){
            notificationAdapter!!.stopListening()
        }
    }

    private fun populateNotificationList(root: View) {
        nDatabase = FirebaseFirestore.getInstance()
        nNotificationRecyclerView = root.findViewById(R.id.notification_list)
        nNotificationRecyclerView.layoutManager = LinearLayoutManager(root.context)

        val notificationQuery: Query = nDatabase.collection("NotificationList").orderBy("title")
        val notificationOptions: FirestoreRecyclerOptions<Notification> =
            FirestoreRecyclerOptions.Builder<Notification>().setQuery(notificationQuery, Notification::class.java).build()

        notificationAdapter = NotificationAdapter(notificationOptions)
        nNotificationRecyclerView.adapter = notificationAdapter
    }

    private fun handleSwipe(){
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
                notificationAdapter!!.deleteItem(viewHolder.adapterPosition)
            }
        }


        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(nNotificationRecyclerView)
    }

    private inner class NotificationView internal constructor(private val view: View) : RecyclerView.ViewHolder(view){
        internal fun setFields(title: String, content: String){
            val titleText = view.findViewById<TextView>(R.id.title)
            titleText.text = title

            val contentText = view.findViewById<TextView>(R.id.content)
            contentText.text = content

        }

    }

    private inner class NotificationAdapter internal constructor(options: FirestoreRecyclerOptions<Notification>) :
        FirestoreRecyclerAdapter<Notification, NotificationView>(options){

        override fun onBindViewHolder(holder: NotificationView, position: Int, model: Notification) {
            holder.setFields(model.title!!, model.content!!)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationView {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_card, parent, false)
            return NotificationView(view)
        }

        fun deleteItem(position: Int) {
            snapshots.getSnapshot(position).reference.delete()
        }
    }
}
