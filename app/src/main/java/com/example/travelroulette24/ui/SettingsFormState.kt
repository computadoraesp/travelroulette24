package com.example.travelroulette24.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ApiCredentialsManager

@Stable
internal class SettingsFormState(
    credentialsManager: ApiCredentialsManager
) {
    var isRealMode by mutableStateOf(credentialsManager.isRealMode)
    var amadeusKey by mutableStateOf(credentialsManager.amadeusApiKey)
    var amadeusSecret by mutableStateOf(credentialsManager.amadeusApiSecret)
    var amadeusIsPaid by mutableStateOf(credentialsManager.amadeusIsPaidTier)
    var amadeusUseProd by mutableStateOf(credentialsManager.amadeusUseProduction)
    var travelPayoutsToken by mutableStateOf(credentialsManager.travelPayoutsToken)

    var kiwiKey by mutableStateOf(credentialsManager.kiwiApiKey)
    var kiwiIsPaid by mutableStateOf(credentialsManager.kiwiIsPaidTier)
    var rapidKey by mutableStateOf(credentialsManager.rapidApiKey)

    var customFlightName by mutableStateOf(credentialsManager.customFlightProviderName)
    var customUrl by mutableStateOf(credentialsManager.customFlightApiUrl)
    var customKey by mutableStateOf(credentialsManager.customFlightApiKey)
    var customHeader by mutableStateOf(credentialsManager.customFlightApiHeader)
    var customFlightEnabled by mutableStateOf(credentialsManager.customFlightApiEnabled)
    var customIsPaid by mutableStateOf(credentialsManager.customFlightIsPaidTier)
    var customDailyLimit by mutableIntStateOf(credentialsManager.customFlightDailyLimit)
    var customFlightNotes by mutableStateOf(credentialsManager.customFlightNotes)

    var customTrainName by mutableStateOf(credentialsManager.customTrainProviderName)
    var customTrainUrl by mutableStateOf(credentialsManager.customTrainApiUrl)
    var customTrainHeader by mutableStateOf(credentialsManager.customTrainApiHeader)
    var customTrainKey by mutableStateOf(credentialsManager.customTrainApiKey)
    var customTrainEnabled by mutableStateOf(credentialsManager.customTrainApiEnabled)
    var customTrainIsPaid by mutableStateOf(credentialsManager.customTrainIsPaidTier)
    var customTrainDailyLimit by mutableIntStateOf(credentialsManager.customTrainDailyLimit)
    var customTrainNotes by mutableStateOf(credentialsManager.customTrainNotes)

    var customBoatName by mutableStateOf(credentialsManager.customBoatProviderName)
    var customBoatUrl by mutableStateOf(credentialsManager.customBoatApiUrl)
    var customBoatHeader by mutableStateOf(credentialsManager.customBoatApiHeader)
    var customBoatKey by mutableStateOf(credentialsManager.customBoatApiKey)
    var customBoatEnabled by mutableStateOf(credentialsManager.customBoatApiEnabled)
    var customBoatIsPaid by mutableStateOf(credentialsManager.customBoatIsPaidTier)
    var customBoatDailyLimit by mutableIntStateOf(credentialsManager.customBoatDailyLimit)
    var customBoatNotes by mutableStateOf(credentialsManager.customBoatNotes)

    var geminiKey by mutableStateOf(credentialsManager.geminiApiKey)
    var aviationKey by mutableStateOf(credentialsManager.aviationStackKey)
    var exchangeRateKey by mutableStateOf(credentialsManager.exchangeRateKey)
    var tripHubUrl by mutableStateOf(credentialsManager.tripHubUrl)

    var navitiaToken by mutableStateOf(credentialsManager.navitiaToken)
    var openHafas by mutableStateOf(credentialsManager.openHafasEnabled)
    var openTripMapKey by mutableStateOf(credentialsManager.openTripMapKey)
    var unsplashKey by mutableStateOf(credentialsManager.unsplashKey)

    var selectedCurrency by mutableStateOf(credentialsManager.currency)
    var searchRadius by mutableIntStateOf(credentialsManager.searchRadiusKm)

    val hasActiveKeys: Boolean
        get() = (amadeusKey.isNotBlank() && amadeusSecret.isNotBlank()) ||
            kiwiKey.isNotBlank() || travelPayoutsToken.isNotBlank() || rapidKey.isNotBlank() || customUrl.isNotBlank()

    fun saveTo(credentialsManager: ApiCredentialsManager) {
        credentialsManager.amadeusApiKey = amadeusKey
        credentialsManager.amadeusApiSecret = amadeusSecret
        credentialsManager.amadeusIsPaidTier = amadeusIsPaid
        credentialsManager.amadeusUseProduction = amadeusUseProd
        credentialsManager.travelPayoutsToken = travelPayoutsToken

        credentialsManager.kiwiApiKey = kiwiKey
        credentialsManager.kiwiIsPaidTier = kiwiIsPaid
        credentialsManager.rapidApiKey = rapidKey

        credentialsManager.customFlightProviderName = customFlightName
        credentialsManager.customFlightApiUrl = customUrl
        credentialsManager.customFlightApiKey = customKey
        credentialsManager.customFlightApiHeader = customHeader
        credentialsManager.customFlightApiEnabled = customFlightEnabled
        credentialsManager.customFlightIsPaidTier = customIsPaid
        credentialsManager.customFlightDailyLimit = customDailyLimit
        credentialsManager.customFlightNotes = customFlightNotes

        credentialsManager.customTrainProviderName = customTrainName
        credentialsManager.customTrainApiUrl = customTrainUrl
        credentialsManager.customTrainApiKey = customTrainKey
        credentialsManager.customTrainApiHeader = customTrainHeader
        credentialsManager.customTrainApiEnabled = customTrainEnabled
        credentialsManager.customTrainIsPaidTier = customTrainIsPaid
        credentialsManager.customTrainDailyLimit = customTrainDailyLimit
        credentialsManager.customTrainNotes = customTrainNotes

        credentialsManager.customBoatProviderName = customBoatName
        credentialsManager.customBoatApiUrl = customBoatUrl
        credentialsManager.customBoatApiKey = customBoatKey
        credentialsManager.customBoatApiHeader = customBoatHeader
        credentialsManager.customBoatApiEnabled = customBoatEnabled
        credentialsManager.customBoatIsPaidTier = customBoatIsPaid
        credentialsManager.customBoatDailyLimit = customBoatDailyLimit
        credentialsManager.customBoatNotes = customBoatNotes

        credentialsManager.geminiApiKey = geminiKey
        credentialsManager.aviationStackKey = aviationKey
        credentialsManager.exchangeRateKey = exchangeRateKey
        credentialsManager.tripHubUrl = tripHubUrl

        credentialsManager.navitiaToken = navitiaToken
        credentialsManager.openHafasEnabled = openHafas
        credentialsManager.openTripMapKey = openTripMapKey
        credentialsManager.unsplashKey = unsplashKey

        credentialsManager.currency = selectedCurrency
        credentialsManager.searchRadiusKm = searchRadius
        credentialsManager.isRealMode = isRealMode
    }

    fun clearAll(context: Context, credentialsManager: ApiCredentialsManager) {
        credentialsManager.clearAllKeys()
        amadeusKey = ""; amadeusSecret = ""; amadeusIsPaid = false; amadeusUseProd = false
        travelPayoutsToken = ""; kiwiKey = ""; kiwiIsPaid = false; rapidKey = ""
        customFlightName = context.getString(R.string.custom_provider_default_flight)
        customUrl = ""; customKey = ""; customFlightEnabled = false; customFlightNotes = ""
        customTrainName = context.getString(R.string.custom_provider_default_train)
        customTrainUrl = ""; customTrainKey = ""; customTrainEnabled = false; customTrainNotes = ""
        customBoatName = context.getString(R.string.custom_provider_default_boat)
        customBoatUrl = ""; customBoatKey = ""; customBoatEnabled = false; customBoatNotes = ""
        geminiKey = ""; aviationKey = ""; exchangeRateKey = ""; tripHubUrl = ""
        navitiaToken = ""; openTripMapKey = ""; unsplashKey = ""
    }
}

@Composable
internal fun rememberSettingsFormState(
    credentialsManager: ApiCredentialsManager
): SettingsFormState = remember {
    SettingsFormState(credentialsManager = credentialsManager)
}
