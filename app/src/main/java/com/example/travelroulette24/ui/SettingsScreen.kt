package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    credentialsManager: ApiCredentialsManager,
    quotaGovernor: ApiQuotaGovernor? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    // API Keys state
    var amadeusKey by remember { mutableStateOf(credentialsManager.amadeusApiKey) }
    var amadeusSecret by remember { mutableStateOf(credentialsManager.amadeusApiSecret) }
    var amadeusIsPaid by remember { mutableStateOf(credentialsManager.amadeusIsPaidTier) }

    var kiwiKey by remember { mutableStateOf(credentialsManager.kiwiApiKey) }
    var kiwiIsPaid by remember { mutableStateOf(credentialsManager.kiwiIsPaidTier) }

    var rapidKey by remember { mutableStateOf(credentialsManager.rapidApiKey) }

    // Custom API state
    var customUrl by remember { mutableStateOf(credentialsManager.customApiUrl) }
    var customKey by remember { mutableStateOf(credentialsManager.customApiKey) }
    var customHeader by remember { mutableStateOf(credentialsManager.customApiAuthHeader) }
    var customIsPaid by remember { mutableStateOf(credentialsManager.customApiIsPaidTier) }
    var customDailyLimit by remember { mutableIntStateOf(credentialsManager.customApiDailyLimit) }

    // Additional APIs
    var geminiKey by remember { mutableStateOf(credentialsManager.geminiApiKey) }
    var aviationKey by remember { mutableStateOf(credentialsManager.aviationStackKey) }
    var exchangeRateKey by remember { mutableStateOf(credentialsManager.exchangeRateKey) }

    // Transit & Tourism
    var navitiaToken by remember { mutableStateOf(credentialsManager.navitiaToken) }
    var openHafas by remember { mutableStateOf(credentialsManager.openHafasEnabled) }
    var openTripMapKey by remember { mutableStateOf(credentialsManager.openTripMapKey) }
    var unsplashKey by remember { mutableStateOf(credentialsManager.unsplashKey) }

    // Preferences
    var selectedCurrency by remember { mutableStateOf(credentialsManager.currency) }
    var searchRadius by remember { mutableIntStateOf(credentialsManager.searchRadiusKm) }

    var cacheClearedMessage by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    val hasActiveKeys = (amadeusKey.isNotBlank() && amadeusSecret.isNotBlank()) ||
            kiwiKey.isNotBlank() || rapidKey.isNotBlank() || customUrl.isNotBlank()

    // Quota metrics from Governor
    val amadeusQuota = quotaGovernor?.getQuotaStatus("amadeus", amadeusIsPaid, monthlyQuota = 2000)
    val kiwiQuota = quotaGovernor?.getQuotaStatus("kiwi", kiwiIsPaid, monthlyQuota = 2000)
    val customQuota = quotaGovernor?.getQuotaStatus("custom", customIsPaid, monthlyQuota = 1500, customDailyLimit = customDailyLimit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ajustes y Configuración",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gestión de Cuotas, APIs y Personalizadas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Text(
                            text = "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            credentialsManager.amadeusApiKey = amadeusKey
                            credentialsManager.amadeusApiSecret = amadeusSecret
                            credentialsManager.amadeusIsPaidTier = amadeusIsPaid

                            credentialsManager.kiwiApiKey = kiwiKey
                            credentialsManager.kiwiIsPaidTier = kiwiIsPaid

                            credentialsManager.rapidApiKey = rapidKey

                            credentialsManager.customApiUrl = customUrl
                            credentialsManager.customApiKey = customKey
                            credentialsManager.customApiAuthHeader = customHeader
                            credentialsManager.customApiIsPaidTier = customIsPaid
                            credentialsManager.customApiDailyLimit = customDailyLimit

                            credentialsManager.geminiApiKey = geminiKey
                            credentialsManager.aviationStackKey = aviationKey
                            credentialsManager.exchangeRateKey = exchangeRateKey

                            credentialsManager.navitiaToken = navitiaToken
                            credentialsManager.openHafasEnabled = openHafas
                            credentialsManager.openTripMapKey = openTripMapKey
                            credentialsManager.unsplashKey = unsplashKey

                            credentialsManager.currency = selectedCurrency
                            credentialsManager.searchRadiusKm = searchRadius

                            onSaved()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("settings_save_button")
                    ) {
                        Text("✓ Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Connection Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasActiveKeys) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasActiveKeys) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasActiveKeys) "🟢" else "⚪",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasActiveKeys) "Modo En Vivo (BYOK Conectado)" else "Modo Simulación / Demo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasActiveKeys) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (hasActiveKeys) "Buscando ofertas y precios reales mediante tus cuotas protegidas y APIs configuradas."
                            else "Introduce tus claves gratuitas o API personalizada para activar precios oficiales en tiempo real.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. TOKEN & QUOTA GOVERNOR (Control Inteligente de Cuota Diaria vs Mensual)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛡️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gestor Inteligente de Tokens y Cuotas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        BadgePill(text = "Ahorro Activo")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Protege tus tokens: Ajustado a 13 consultas/día para planes de 500/mes y ~60 consultas/día para planes de 2.000/mes, reservando un colchón de seguridad para giros imprevistos de la ruleta y evitando agotar la cuota mensual. Si el cupo diario se agota, conmuta automáticamente a 'Tokens Libres' (fuentes comunitarias) sin detener la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-time meters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuotaMetricBox(
                            providerName = "Amadeus",
                            isPaid = amadeusIsPaid,
                            requestsToday = amadeusQuota?.requestsToday ?: 0,
                            dailyBudget = amadeusQuota?.dailyBudget ?: 60,
                            requestsMonth = amadeusQuota?.requestsThisMonth ?: 0,
                            monthlyLimit = 2000,
                            modifier = Modifier.weight(1f)
                        )
                        QuotaMetricBox(
                            providerName = "Kiwi",
                            isPaid = kiwiIsPaid,
                            requestsToday = kiwiQuota?.requestsToday ?: 0,
                            dailyBudget = kiwiQuota?.dailyBudget ?: 60,
                            requestsMonth = kiwiQuota?.requestsThisMonth ?: 0,
                            monthlyLimit = 2000,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (customUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        QuotaMetricBox(
                            providerName = "API Personalizada",
                            isPaid = customIsPaid,
                            requestsToday = customQuota?.requestsToday ?: 0,
                            dailyBudget = customQuota?.dailyBudget ?: customDailyLimit,
                            requestsMonth = customQuota?.requestsThisMonth ?: 0,
                            monthlyLimit = 1500,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                quotaGovernor?.clearCache()
                                cacheClearedMessage = true
                            }
                        ) {
                            Text("🔄 Limpiar Caché de Ofertas", fontSize = 12.sp)
                        }

                        if (cacheClearedMessage) {
                            Text(
                                text = "✓ Caché liberada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. API PERSONALIZADA / CUSTOM ENDPOINT (Prioridad del usuario)
            Text(
                text = "⚡ API Personalizada / Servidor Propio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Endpoint Personalizado (B2B / Proxy)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (customUrl.isNotBlank()) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (customUrl.isNotBlank()) "✓ Activo" else "No configurado",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (customUrl.isNotBlank()) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Conecta tu propio servidor REST, proxy corporativo, worker o API privada de vuelos. TravelRoulette24 consultará tu URL pasando tu origen y cabecera de autenticación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("URL del Endpoint (ej: https://api.miviaje.com/v1/search)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_custom_api_url")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customHeader,
                            onValueChange = { customHeader = it },
                            label = { Text("Nombre Cabecera") },
                            placeholder = { Text("Authorization o X-Api-Key") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customKey,
                            onValueChange = { customKey = it },
                            label = { Text("Token / API Key") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Plan Mode Selector for Custom API
                    PlanTierSelector(
                        isPaid = customIsPaid,
                        onPlanChange = { customIsPaid = it },
                        freeDescription = "Plan Gratuito: límite de seguridad diario para no saturar tu servidor",
                        paidDescription = "Plan de Pago / Ilimitado: sin límite diario, barridos completos en tiempo real"
                    )

                    if (!customIsPaid) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Límite diario personalizado:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(20, 50, 100).forEach { limit ->
                                    val isSelected = customDailyLimit == limit
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                            .clickable { customDailyLimit = limit }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$limit/día",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // 4. VUELOS Y AGREGADORES CON SELECTOR DE PLAN (GRATIS VS PAGO)
            Text(
                text = "✈️ Búsqueda de Vuelos (Amadeus & Kiwi)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Provider: Amadeus Self-Service
            SettingsApiCard(
                title = "Amadeus Self-Service",
                badge = if (amadeusIsPaid) "Plan de Pago (Ilimitado)" else "2.000 req/mes gratis (~60/día)",
                description = "Precios oficiales y vuelos comerciales globales (Flight Inspiration Search).",
                isConfigured = amadeusKey.isNotBlank() && amadeusSecret.isNotBlank(),
                onGetFreeKey = { openUrl("https://developers.amadeus.com/register") }
            ) {
                PlanTierSelector(
                    isPaid = amadeusIsPaid,
                    onPlanChange = { amadeusIsPaid = it },
                    freeDescription = "Plan Gratuito: 2.000 req/mes (Protegido a ~60 peticiones/día máx, con reserva para giros)",
                    paidDescription = "Plan de Pago Enterprise: Sin límite diario. Barridos en vivo sin restricción."
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amadeusKey,
                    onValueChange = { amadeusKey = it },
                    label = { Text("Amadeus API Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_amadeus_key")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amadeusSecret,
                    onValueChange = { amadeusSecret = it },
                    label = { Text("Amadeus API Secret") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_amadeus_secret")
                )
            }

            // Provider: Kiwi Tequila
            SettingsApiCard(
                title = "Kiwi.com (Tequila API)",
                badge = if (kiwiIsPaid) "Plan de Pago (Ilimitado)" else "Gratis con registro (~60/día)",
                description = "Especialista en aerolíneas low-cost (Ryanair, EasyJet, Vueling) y rutas 'A Cualquier Lugar'.",
                isConfigured = kiwiKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://tequila.kiwi.com/portal/login/register") }
            ) {
                PlanTierSelector(
                    isPaid = kiwiIsPaid,
                    onPlanChange = { kiwiIsPaid = it },
                    freeDescription = "Plan Gratuito: Protegido a ~60 peticiones/día máx con reserva para giros",
                    paidDescription = "Plan de Pago Tequila Pro: Acceso ilimitado a tarifas en tiempo real."
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = kiwiKey,
                    onValueChange = { kiwiKey = it },
                    label = { Text("Tequila API Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_kiwi_key")
                )
            }

            // Provider: RapidAPI
            SettingsApiCard(
                title = "RapidAPI (Google Flights & Skyscanner)",
                badge = "Cuota gratis por mes",
                description = "Una sola cuenta para múltiples agregadores de vuelos mundiales.",
                isConfigured = rapidKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://rapidapi.com/auth/sign-up") }
            ) {
                OutlinedTextField(
                    value = rapidKey,
                    onValueChange = { rapidKey = it },
                    label = { Text("X-RapidAPI-Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_rapidapi_key")
                )
            }

            HorizontalDivider()

            // 5. NUEVAS APIS: INTELIGENCIA ARTIFICIAL, RADAR Y DIVISAS
            Text(
                text = "✨ Nuevas APIs de Inteligencia y En Vivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Google Gemini AI
            SettingsApiCard(
                title = "Google Gemini AI (Itinerarios 24h Express)",
                badge = "Cuota Gratis de Google AI Studio",
                description = "Genera planes paso a paso de qué hacer, dónde comer y cómo moverse en 24h para cada oferta de la ruleta.",
                isConfigured = geminiKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://aistudio.google.com/app/apikey") }
            ) {
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Google Gemini API Key (AIzaSy...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_gemini_key")
                )
            }

            // AviationStack
            SettingsApiCard(
                title = "AviationStack (Radar de Salidas en Directo)",
                badge = "500 req/mes gratis (~13/día)",
                description = "Consulta la pantalla de salidas en directo de aeropuertos para vuelos que despegan en 3 a 12 horas.",
                isConfigured = aviationKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://aviationstack.com/signup/free") }
            ) {
                OutlinedTextField(
                    value = aviationKey,
                    onValueChange = { aviationKey = it },
                    label = { Text("AviationStack Access Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_aviation_key")
                )
            }

            // ExchangeRate-API
            SettingsApiCard(
                title = "ExchangeRate-API (Conversor de Moneda en Vivo)",
                badge = "1.500 req/mes gratis",
                description = "Actualiza tipos de cambio automáticos para destinos fuera de la eurozona (GBP, CHF, USD, TRY, MAD).",
                isConfigured = exchangeRateKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://www.exchangerate-api.com/") }
            ) {
                OutlinedTextField(
                    value = exchangeRateKey,
                    onValueChange = { exchangeRateKey = it },
                    label = { Text("ExchangeRate API Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_exchangerate_key")
                )
            }

            HorizontalDivider()

            // 6. TRENES Y TRANSPORTE TERRESTRE (CON TOKENS LIBRES)
            Text(
                text = "🚆 Trenes y Transporte Terrestre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Provider: Navitia
            SettingsApiCard(
                title = "Navitia.io (Trenes en Europa)",
                badge = "50.000 req/mes gratis",
                description = "Redes ferroviarias de Renfe, SNCF, Deutsche Bahn y Eurostar.",
                isConfigured = navitiaToken.isNotBlank(),
                onGetFreeKey = { openUrl("https://www.navitia.io/register/") }
            ) {
                OutlinedTextField(
                    value = navitiaToken,
                    onValueChange = { navitiaToken = it },
                    label = { Text("Token Navitia") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_navitia_token")
                )
            }

            // Provider: Open HAFAS (Open source / Tokens Libres)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Open HAFAS (Token Libre / Comunitario)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BadgePill(text = "100% Libre")
                        }
                        Text(
                            text = "Red abierta sin coste de horarios de trenes en Europa. Actúa como respaldo automático cuando tus cuotas de APIs se agotan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = openHafas,
                        onCheckedChange = { openHafas = it }
                    )
                }
            }

            HorizontalDivider()

            // 7. DESTINATION INTELLIGENCE
            Text(
                text = "🏛️ Turismo y Fotos de Destino",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // OpenTripMap
            SettingsApiCard(
                title = "OpenTripMap",
                badge = "Gratis",
                description = "Puntos de interés, monumentos y atractivos turísticos para ver en 24h.",
                isConfigured = openTripMapKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://opentripmap.io/product") }
            ) {
                OutlinedTextField(
                    value = openTripMapKey,
                    onValueChange = { openTripMapKey = it },
                    label = { Text("OpenTripMap API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Unsplash
            SettingsApiCard(
                title = "Unsplash API",
                badge = "50 fotos/hora gratis",
                description = "Fotografías en alta resolución de las ciudades de destino.",
                isConfigured = unsplashKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://unsplash.com/developers") }
            ) {
                OutlinedTextField(
                    value = unsplashKey,
                    onValueChange = { unsplashKey = it },
                    label = { Text("Unsplash Access Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // 8. PREFERENCIAS DE BÚSQUEDA
            Text(
                text = "⚙️ Preferencias de Búsqueda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Moneda Predeterminada",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EUR" to "€ Euro", "USD" to "$ Dólar", "GBP" to "£ Libra").forEach { (code, label) ->
                            val selected = selectedCurrency == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCurrency = code }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Radio de Búsqueda de Aeropuertos / Estaciones",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50 to "50 km", 100 to "100 km", 150 to "150 km", 250 to "250 km").forEach { (km, label) ->
                            val selected = searchRadius == km
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { searchRadius = km }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // 9. PRIVACIDAD Y BORRADO SEGURO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacidad y Almacenamiento Local Seguro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tus credenciales y URLs privadas nunca se transmiten a servidores externos ni intermediarios. Se almacenan únicamente en el almacenamiento protegido de tu dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            credentialsManager.clearAllKeys()
                            amadeusKey = ""
                            amadeusSecret = ""
                            amadeusIsPaid = false
                            kiwiKey = ""
                            kiwiIsPaid = false
                            rapidKey = ""
                            customUrl = ""
                            customKey = ""
                            geminiKey = ""
                            aviationKey = ""
                            exchangeRateKey = ""
                            navitiaToken = ""
                            openTripMapKey = ""
                            unsplashKey = ""
                            quotaGovernor?.clearCache()
                            onSaved()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_keys_button")
                    ) {
                        Text("🗑️ Borrar todas las claves y configuraciones")
                    }
                }
            }

            // FOOTER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TravelRoulette24 • Arquitectura BYOK con Gobernador de Tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Viajes espontáneos en menos de 24 horas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuotaMetricBox(
    providerName: String,
    isPaid: Boolean,
    requestsToday: Int,
    dailyBudget: Int,
    requestsMonth: Int,
    monthlyLimit: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPaid) "PRO" else "FREE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPaid) "Hoy: $requestsToday (Ilimitado)"
                else "Hoy: $requestsToday / $dailyBudget req",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPaid) "Mes: $requestsMonth req"
                else "Mes: $requestsMonth / $monthlyLimit req",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanTierSelector(
    isPaid: Boolean,
    onPlanChange: (Boolean) -> Unit,
    freeDescription: String,
    paidDescription: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (!isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onPlanChange(false) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🟢 Plan Gratuito (Cuota segura)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (!isPaid) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPaid) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onPlanChange(true) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💎 Plan de Pago (Ilimitado)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPaid) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isPaid) paidDescription else freeDescription,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsApiCard(
    title: String,
    badge: String,
    description: String,
    isConfigured: Boolean,
    onGetFreeKey: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isConfigured) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isConfigured) "✓ Configurada" else "Sin clave",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConfigured) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgePill(text = badge)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onGetFreeKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔗 Conseguir clave gratis en portal oficial", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
