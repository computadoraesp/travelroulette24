package com.example.travelroulette24.ui

import com.example.travelroulette24.engine.TravelIntelligenceEngine

/**
 * Evaluates the optional minimum-distance veto rule:
 * filters trips to `distanceKm >= minDistanceKm` ONLY when at least one historical
 * price record (either an engine/backend opportunity route or a prior SQLite record)
 * covers that minimum distance.
 */
object DistanceVetoHelper {

    data class VetoEvaluation(
        val filteredResults: List<TravelIntelligenceEngine.ResultItem>,
        val hasHistoricalCoverage: Boolean
    )

    fun evaluate(
        items: List<TravelIntelligenceEngine.ResultItem>,
        originLat: Double?,
        originLon: Double?,
        historicalLowByHub: Map<String, Double>,
        enabled: Boolean,
        minDistanceKm: Int
    ): VetoEvaluation {
        if (!enabled) {
            return VetoEvaluation(filteredResults = items, hasHistoricalCoverage = true)
        }

        val hasCoverageFromResults = items.any { item ->
            if (!item.isOpportunity) return@any false
            val dist = resolveDistanceKm(item, originLat, originLon) ?: return@any false
            dist >= minDistanceKm
        }

        val fallbackOriginHub = items.firstOrNull()?.originHub
        val hasCoverageFromDb = historicalLowByHub.keys.any { destKey ->
            val dist = when {
                originLat != null && originLon != null ->
                    HubCoordinates.calculateDistanceKmOrNull(originLat, originLon, destKey)
                fallbackOriginHub != null ->
                    HubCoordinates.calculateDistanceKmOrNull(fallbackOriginHub, destKey)
                else -> null
            }
            dist != null && dist >= minDistanceKm
        }

        val hasCoverage = hasCoverageFromResults || hasCoverageFromDb
        if (!hasCoverage) {
            return VetoEvaluation(filteredResults = items, hasHistoricalCoverage = false)
        }

        val filtered = items.filter { item ->
            val dist = resolveDistanceKm(item, originLat, originLon) ?: return@filter false
            dist >= minDistanceKm
        }
        return VetoEvaluation(filteredResults = filtered, hasHistoricalCoverage = true)
    }

    private fun resolveDistanceKm(
        item: TravelIntelligenceEngine.ResultItem,
        originLat: Double?,
        originLon: Double?
    ): Int? {
        val hubDist = HubCoordinates.calculateDistanceKmOrNull(item.originHub, item.destinationHub)
        if (hubDist != null && hubDist > 0) return hubDist
        if (originLat != null && originLon != null) {
            return HubCoordinates.calculateDistanceKmOrNull(originLat, originLon, item.destinationHub)
        }
        return null
    }
}
