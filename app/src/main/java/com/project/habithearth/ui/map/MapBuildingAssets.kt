package com.project.habithearth.ui.map

/**
 * Asset paths under `app/src/main/assets/` for map marker images.
 *
 * Markers are chosen by the building's `id` (from [VillageBuilding]).
 *
 * The "image folder naming/category grouping" is just organization — this code only cares about
 * `buildingId` and the explicit image paths you configure below.
 *
 * Important: Android asset paths are case-sensitive on device. Update these strings to match
 * your files exactly (including `.PNG` vs `.png`).
 */

private data class BuildingMarkerArt(val image: String)

private val BuildingMarkerArtById: Map<String, BuildingMarkerArt> = mapOf(
    // Replace these examples with your actual per-building assets.
    "library" to BuildingMarkerArt(
        image = "images/buildings/wisdom/observatory.PNG",
    ),
    "cottage" to BuildingMarkerArt(
        image = "images/building_towers.PNG",
    ),
    "spa" to BuildingMarkerArt(
        image = "images/buildings/vitality/hotspring.PNG",
    ),
    "guild" to BuildingMarkerArt(
        image = "images/buildings/strength/tower.PNG",
    ),
    "greenhouse" to BuildingMarkerArt(
        image = "images/buildings/spirit/bakery.PNG",
    ),
)

private val DefaultBuildingMarkerArt = BuildingMarkerArt(
    image = "images/buildings/wisdom/observatory.PNG",
)

/** `buildingIndex` is ignored; kept to avoid changing call sites. */
fun markerAssetPathForBuilding(buildingId: String, buildingIndex: Int): String {
    return (BuildingMarkerArtById[buildingId] ?: DefaultBuildingMarkerArt).image
}
