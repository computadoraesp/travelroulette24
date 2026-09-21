package com.example.travelroulette24

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.travelroulette24.location.LocalLocationHubResolver
import com.example.travelroulette24.network.HubResponse
import com.example.travelroulette24.network.TripHubHelper
import com.example.travelroulette24.ui.LocationInfo
import com.example.travelroulette24.ui.theme.TravelRoulette24Theme
import com.example.travelroulette24.utils.QRCodeUtil
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val locationResolver by lazy { LocalLocationHubResolver(this) }
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fetchLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialise PreferenceHelper
        PreferenceHelper.init(this)
        // Request location permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }
        setContent {
            // UI state holders
            var city by remember { mutableStateOf<String?>(null) }
            var hubs by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
            var budget by remember { mutableStateOf(PreferenceHelper.getBudget() ?: 0) }
            var centralized by remember { mutableStateOf(false) }
            var requiredCount by remember { mutableStateOf(0) }
            var hubId by remember { mutableStateOf<String?>(null) }
            var shortUrl by remember { mutableStateOf<String?>(null) }
            var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var responses by remember { mutableStateOf<List<HubResponse>>(emptyList()) }
            val coroutineScope = rememberCoroutineScope()

            // Callback for location resolution
            RegisterLocationCallback { resolvedCity, resolvedHubs ->
                city = resolvedCity
                hubs = resolvedHubs
            }

            TravelRoulette24Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                        TravelRouletteMainApp(name = "TravelRoulette24")
                        // Budget slider UI
                        Text(text = "Maximum budget: $${budget}")
                        Slider(
                            value = budget.toFloat(),
                            onValueChange = { budget = it.toInt() },
                            valueRange = 0f..1000f,
                            onValueChangeFinished = { PreferenceHelper.setBudget(budget) }
                        )
                        // Centralized booking toggle
                        Row {
                            Text("Centralized booking (organizer purchases tickets)")
                            Switch(checked = centralized, onCheckedChange = { centralized = it })
                        }
                        // Required participants count
                        Text(text = "Required participants (0 = no minimum): $requiredCount")
                        Slider(
                            value = requiredCount.toFloat(),
                            onValueChange = { requiredCount = it.toInt() },
                            valueRange = 0f..10f,
                            steps = 9,
                            onValueChangeFinished = {}
                        )
                        // Invite friends button
                        Button(onClick = {
                            coroutineScope.launch {
                                try {
                                    val (id, url) = TripHubHelper.createHub(
                                        offerId = "demo_offer",
                                        originCity = city ?: "unknown",
                                        hubCodes = hubs,
                                        ttlHours = 24,
                                        centralized = centralized,
                                        requiredCount = requiredCount
                                    )
                                    hubId = id
                                    shortUrl = url
                                    qrBitmap = QRCodeUtil.generate(url)
                                    Toast.makeText(this@MainActivity, "Hub created: $url", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(this@MainActivity, "Failed to create hub", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Text("Invite friends")
                        }
                        // Show QR code if available
                        qrBitmap?.let { bmp ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR code for hub")
                        }
                        // Show short link for sharing
                        shortUrl?.let { url ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Share link: $url")
                        }
                        // Poll responses if hub created
                        hubId?.let { id ->
                            LaunchedEffect(id) {
                                TripHubHelper.pollResponses(id).collect { list ->
                                    responses = list
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Participants (${responses.size}):")
                            responses.forEach { resp ->
                                Text(text = "- ${resp.nickname}: ${resp.answer}${if (!resp.name.isNullOrEmpty()) ", name: ${resp.name}" else ""}")
                            }
                        }
                        // Display location info
                        LocationInfo(city = city, hubs = hubs)
                    }
                }
            }
        }
    }

    private fun fetchLocation() {
        try {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                // Permissions are missing; gracefully fall back to default coordinates (e.g. Paris center)
                locationResolver.getCityFromCoordinates(48.8566, 2.3522) { city ->
                    val nearest = locationResolver.findNearestHubs(48.8566, 2.3522)
                    locationCallback?.invoke(city, nearest)
                }
                return
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    locationResolver.getCityFromCoordinates(location.latitude, location.longitude) { city ->
                        val nearest = locationResolver.findNearestHubs(location.latitude, location.longitude)
                        locationCallback?.invoke(city, nearest)
                    }
                } else {
                    // Fall back gracefully if last location is null
                    locationResolver.getCityFromCoordinates(48.8566, 2.3522) { city ->
                        val nearest = locationResolver.findNearestHubs(48.8566, 2.3522)
                        locationCallback?.invoke(city, nearest)
                    }
                }
            }.addOnFailureListener {
                // Fall back gracefully on failure
                locationResolver.getCityFromCoordinates(48.8566, 2.3522) { city ->
                    val nearest = locationResolver.findNearestHubs(48.8566, 2.3522)
                    locationCallback?.invoke(city, nearest)
                }
            }
        } catch (_: SecurityException) {
            // Guard against unexpected SecurityExceptions explicitly
            locationResolver.getCityFromCoordinates(48.8566, 2.3522) { city ->
                val nearest = locationResolver.findNearestHubs(48.8566, 2.3522)
                locationCallback?.invoke(city, nearest)
            }
        }
    }

    companion object {
        // Simple holder to bridge resolver callback to composable state
        var locationCallback: ((String, Map<String, List<String>>) -> Unit)? = null
    }
}

@Composable
fun TravelRouletteMainApp(name: String, modifier: Modifier = Modifier) {
    Text(text = "Welcome to $name!", modifier = modifier)
}

@Composable
private fun RegisterLocationCallback(onResolved: (String, Map<String, List<String>>) -> Unit) {
    LaunchedEffect(Unit) {
        MainActivity.locationCallback = onResolved
    }
}