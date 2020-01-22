package dal.cs.mc.routefit


import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil
import dal.cs.mc.routefit.helpers.MapsDrawingFunctionality
import dal.cs.mc.routefit.helpers.RouteInformation
import dal.cs.mc.routefit.helpers.TextToSpeechFunctionality
import dal.cs.mc.routefit.models.Exercise
import dal.cs.mc.routefit.models.Notification
import dal.cs.mc.routefit.models.User
import org.json.JSONArray

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    override fun onMarkerClick(p0: Marker?) = false
    val TAG = "MapsActivity"
    val GSERVICES_API_KEY = "AIzaSyC56TXxsY2oIQsK6IfeIcYsuV4zsrksvLQ"
    val GMAP_URL = "https://maps.googleapis.com/maps/api/directions/json"
    private lateinit var nDatabase: FirebaseFirestore
    private lateinit var user: User
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationState = false
    private lateinit var currLocation: LatLng
    private lateinit var destLocation: LatLng
    private lateinit var destMarker: Marker
    private lateinit var autocompleteSFragment: AutocompleteSupportFragment
    private lateinit var drawPath: FloatingActionButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout?>
    private lateinit var mapsDrawingFunctionality: MapsDrawingFunctionality
    private lateinit var textToSpeech: TextToSpeechFunctionality
    private lateinit var bStartRoute: Button
    private lateinit var mRouteInformation: RouteInformation
    private lateinit var polylines: ArrayList<Polyline>
    private lateinit var checkpoints: ArrayList<LatLng>
    private lateinit var checkpointsMaker: ArrayList<Circle>

    private var useVoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        useVoice = false
        mapsDrawingFunctionality = MapsDrawingFunctionality()
        textToSpeech = TextToSpeechFunctionality()
        textToSpeech.mainThreadInit(applicationContext)
        mRouteInformation = RouteInformation()
        polylines = ArrayList()
        checkpointsMaker = ArrayList()
        checkpoints = ArrayList()
        nDatabase = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        drawPath = findViewById(R.id.draw_path)
        bottomSheet = findViewById(R.id.bottom_sheet)
        sheetBehavior = BottomSheetBehavior.from(this.bottomSheet)
        bStartRoute = findViewById(R.id.start_routing)
        locationRequest()
        initBackButton()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, GSERVICES_API_KEY)
        }
        autocompleteSFragment =
            supportFragmentManager.findFragmentById(R.id.place_autocomplete) as AutocompleteSupportFragment
        autocompleteSFragment.setPlaceFields(
            arrayOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG).toList()
        )
        autocompleteSFragment.setOnPlaceSelectedListener(
            object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    Log.i(TAG, "Place:" + place.name + ", " + place.id)
                    println("${place.latLng?.latitude}, ${place.latLng?.latitude}")
                    destLocation = place.latLng!!
                    mMap.addMarker(
                        MarkerOptions().position(place.latLng!!)
                            .title(place.name)
                            .draggable(true)
                    )
                }

                override fun onError(status: Status) {
                    Log.i(TAG, "An error occurred: $status")
                }
            })
        locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(loc: LocationResult?) {
                    super.onLocationResult(loc)
                    loc ?: return
                    for (location in loc.locations) {
                        currLocation = LatLng(location.latitude, location.longitude)
                        //move camera to user's location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currLocation, 13f))
                        //iterate
                        if (useVoice && !mRouteInformation.checkpoints.isEmpty()) {
                            //get first checkpoint
                            val checkpoint = mRouteInformation.checkpoints[0]

                            val distance =
                                location.distanceTo(Location(LocationManager.GPS_PROVIDER).apply {
                                    latitude = checkpoint.latitude
                                    longitude = checkpoint.longitude
                                })
                            //calculate distance to checkpoint
                            println(distance)
                            //if less, say it
                            if (distance < 100) {
                                textToSpeech.speakLine(mRouteInformation.instructions.removeAt(0))
                                mRouteInformation.checkpoints.removeAt(0)
                            } else {
                                textToSpeech.speakLine(mRouteInformation.instructions.get(0))
                                textToSpeech.silence()
                            }
                        }
                        mMap.setOnMapClickListener {
                            destLocation = it
                            if (::destMarker.isInitialized) {
                                destMarker.remove()
                            }
                            for (line in polylines) {
                                line.remove()
                            }
                            for (check in checkpointsMaker) {
                                check.remove()
                            }
                            checkpoints.clear()
                            polylines.clear()
                            checkpoints
                            destMarker =
                                mMap.addMarker(MarkerOptions().position(destLocation).draggable(true))
                        }
                    }
                }
            }
        notificationChannel()
        drawPath.setOnClickListener {
            if (::destLocation.isInitialized) {
                drawPathOnMaps(currLocation, destLocation)
                if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            } else
                Toast.makeText(this, "Select a Destination", Toast.LENGTH_LONG).show()
        }
        bStartRoute.setOnClickListener { view ->
            showNotification("Maps", "Starting route to "+ mRouteInformation.address)
            useVoice = !useVoice

        }
    }

    fun showNotification(title: String, content: String) {

        /**
         * Send notification data to firebase for display in the notifications fragment
         */
        val documentReference: DocumentReference = nDatabase.collection("NotificationList").document()
        val notification = Notification(title, content)
        notification.id = documentReference.id
        documentReference.set(notification)

        /**
         * Build notification
         */
        var builder = NotificationCompat.Builder(this, "RouteFit")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(342, builder.build())
        }

    }

    private fun notificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "route fit"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("RouteFit", channelName, importance)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }


    private fun initBackButton() {
        //next two lines needed for back button
        assert(supportActionBar != null)   //null check
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)   //show back button
    }

    /**
     * This function draws the route selected on the map and draws small markers in spots where
     * Google Directions API says there is a turn to be made. When the user is close to this point
     * this is when a voiced direction will be triggered.
     */
    private fun drawPathOnMaps(currLocation: LatLng, destLocation: LatLng) {
        val path: MutableList<List<LatLng>> = ArrayList()
        val instructions: MutableList<String> = ArrayList()
        var stepsIterator: JSONArray
        val urlDirections =
            "$GMAP_URL?origin=${currLocation.latitude},${currLocation.longitude}&destination=${destLocation.latitude},${destLocation.longitude}&key=$GSERVICES_API_KEY&mode=walking"
        println(urlDirections)
        val directionsRequest =
            object : StringRequest(
                Method.GET, urlDirections,
                Response.Listener<String> { response ->
                    stepsIterator = mapsDrawingFunctionality.getStringInstructions(response)
                    val steps = stepsIterator.getJSONObject(0).getJSONArray("steps")
                    for (i in 0 until steps.length()) {
                        val points =
                            steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                        path.add(PolyUtil.decode(points))
                        instructions.add(steps.getJSONObject(i).getString("html_instructions"))
                        val chklat =
                            steps.getJSONObject(i).getJSONObject("end_location").getDouble("lat")
                        val chklng =
                            steps.getJSONObject(i).getJSONObject("end_location").getDouble("lng")
                        checkpoints.add(LatLng(chklat, chklng))
                        println("$i, $chklat, $chklng")
                    }
//                   remove destination marker
                    if (::destMarker.isInitialized) {
                        destMarker.remove()
                    }
                    destMarker = mMap.addMarker(
                        MarkerOptions().position(destLocation)
                            .draggable(true)
                    )
                    for (loc in checkpoints) {

                        val chckMrk = mMap.addCircle(
                            CircleOptions().center(loc)
                                .radius(50.0).fillColor(Color.BLACK)
                                .strokeWidth(2f)
                        )
                        checkpointsMaker.add(chckMrk)
                    }
                    for (i in 0 until path.size) {
                        val pLine = mMap.addPolyline(
                            PolylineOptions().addAll(path[i])
                                .color(Color.BLUE).width(8f).clickable(true)
                        )
                        polylines.add(pLine)
                    }

                    mRouteInformation.refactorData(stepsIterator)
                    mRouteInformation.setInstructions(instructions)
                    mRouteInformation.setCheckpoints(checkpoints)
                    updateBottomSheet(mRouteInformation)

                    destMarker.title = mRouteInformation.address

                    sendRunToDatabase(
                        "Run",
                        mRouteInformation.caloriesInt,
                        mRouteInformation.timeMin
                    )

                }, Response.ErrorListener
                {
                    Log.e(TAG, it.toString())

                }) {}

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(directionsRequest)
    }

    /**
     * This function updates the bottom sheet UI fields with info returned from the
     * Google Directions API call
     */
    private fun updateBottomSheet(routeInformation: RouteInformation) {
        findViewById<TextView>(R.id.dest_address).text = routeInformation.address
        findViewById<TextView>(R.id.time_taken).text = routeInformation.timetaken
        findViewById<TextView>(R.id.city).text = routeInformation.city
        findViewById<TextView>(R.id.post_code).text = routeInformation.postalCode
        findViewById<TextView>(R.id.calories).text = routeInformation.calories
        val lstView: ListView = findViewById(R.id.list_instructions)
        val listItems = arrayOfNulls<String>(routeInformation.instructions.size)
        for (i in 0 until routeInformation.instructions.size)
            listItems[i] = routeInformation.instructions[i]
        lstView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        //enable zooming through controls or through gestures
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isZoomGesturesEnabled = true


        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter())

        mMap.setOnMarkerClickListener(this)
        drawMarkers()
        mapSetup()


    }


//check location permissions and request from the user if necessary

    private fun mapSetup() {
        checkPerms()
        //enable location
        mMap.isMyLocationEnabled = true

        //get last known location from fused location client
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val latlng = LatLng(location.latitude, location.longitude)

                //move camera to users location
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 10f))
            }
        }
    }

    private fun checkPerms() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            //Request permissions from the user to use location
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }
    }


    //start continuous location updates after checking perms
    private fun startUpdates() {
        checkPerms()
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun locationRequest() {
        locationRequest = LocationRequest()
        //set intervals for request frequency, and priority of location type
        locationRequest.interval = 20000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        //build the location request
        val requestBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val request = client.checkLocationSettings(requestBuilder.build())

        //if location settings are fine, we can start making location updates
        request.addOnSuccessListener {
            locationState = true
            startUpdates()
        }
        //otherwise we need to show the user a dialog to ensure they have the proper settings
        request.addOnFailureListener { error ->
            if (error is ResolvableApiException) {
                //try a dialogue
                try {
                    error.startResolutionForResult(this@MapsActivity, 2)
                } catch (sendEx: IntentSender.SendIntentException) {

                }

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                locationState = true
                startUpdates()
            }
        }
    }

    //pause location updates when app is running in the background
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationState) {
            startUpdates()
        }
    }

    /**
     * This function pulls data of application users down from firebase
     * and displays markers on the map that represent the users based on
     * current user's settings
     */
    fun drawMarkers() {

        nDatabase.collection("UserList").get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    user = doc.toObject(User::class.java)
                    //if the user's status is 'looking for partner'

                    if (user.status == 0 && user.preference == UserPreferences.preference) {
                        // Log.d("Log1", user.name)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(
                                    LatLng(
                                        user.location!!.latitude,
                                        user.location!!.longitude
                                    )
                                )
                                .title(user.name)
                                .snippet(user.preference)
                                .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.usericon)))
                                .infoWindowAnchor(0.5f, 0.5f)
                        )

                        //TODO: Wrap this in an if checking UserPreferences for partner notifications (DONE)
                        if(UserPreferences.status == 0){
                            showNotification("Partner Nearby", user.name+" is looking for a "+user.preference+" partner.")
                        }

                    }
                }
            }.addOnFailureListener { exception ->
                Log.d("Debug", "", exception)
            }

    }


    /**
     * This function allows us to send the data for the run to the database so we can display it
     * in the user's exercise list
     */
    private fun sendRunToDatabase(name: String, calories: Int, time: Int) {
        val documentReference: DocumentReference = nDatabase.collection("ExerciseList").document()
        val ex = Exercise(name, 0)
        ex.id = documentReference.id
        ex.calories = calories
        ex.time = time.toDouble()
        documentReference.set(ex)
    }


    private inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        private val infoWindow: View = layoutInflater.inflate(R.layout.map_marker_info_window, null)
        private val windowContents: View =
            layoutInflater.inflate(R.layout.info_window_contents, null)

        override fun getInfoWindow(p0: Marker?): View {
            showWindow(p0, infoWindow)
            return infoWindow
        }

        override fun getInfoContents(p0: Marker?): View {
            showWindow(p0, windowContents)
            return windowContents
        }
    }

    /**
     * This function draws pictures and text on the info window of the supplied marker.
     */
    private fun showWindow(marker: Marker?, view: View) {
        if (marker != null) {
            val profilePicture = when (marker.title) {
                "Bob" -> R.drawable.camera
                "Jane" -> R.drawable.turtle
                "Chad" -> R.drawable.man
                "Jack" -> R.drawable.fish
                "Stacey" -> R.drawable.dog
                "Bill" -> R.drawable.grey
                "Lea" -> R.drawable.flower
                else -> 0
            }

            //set window image to profile picture
            view.findViewById<ImageView>(R.id.profilepic).setImageResource(profilePicture)

            //set username to marker title
            val title = marker.title
            if (title != null) {
                view.findViewById<TextView>(R.id.username).text = title
            } else {
                view.findViewById<TextView>(R.id.username).text = ""
            }

            //set user status. Note that all visible markers have status set to visible (0)
            //so we only need to set the textview to show this
            //Here we also show the type of workout partner by displaying the snippet text
            val snippet = marker.snippet
            if (snippet != null) {
                view.findViewById<TextView>(R.id.status).text = "Wants " + snippet + " partner"
            } else {
                view.findViewById<TextView>(R.id.status).text = ""
            }

        }
    }

    override fun onSupportNavigateUp(): Boolean { //back button on action bar
        finish()
        return true
    }
}


