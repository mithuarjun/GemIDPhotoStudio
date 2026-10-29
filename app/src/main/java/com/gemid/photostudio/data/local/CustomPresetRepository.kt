package com.gemid.photostudio.data.local

import android.content.Context
import android.content.SharedPreferences
import com.gemid.photostudio.data.model.CustomPhotoSpec
import com.gemid.photostudio.sheet.PrintPaperDefinition
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent repository managing user-defined custom photo specifications and
 * custom paper size templates using local SharedPreferences.
 * 
 * Persists across app launches and device reboots so photo studios can create
 * and maintain customized templates for recurring client jobs.
 */
class CustomPresetRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ── Custom Photo Specifications ─────────────────────────────────

    fun getCustomPhotoSpecs(): List<CustomPhotoSpec> {
        val jsonStr = prefs.getString(KEY_CUSTOM_PHOTO_SPECS, null) ?: return emptyList()
        val result = mutableListOf<CustomPhotoSpec>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    CustomPhotoSpec(
                        id = obj.optString("id", "custom_${System.currentTimeMillis()}_$i"),
                        title = obj.optString("title", "Custom Size"),
                        countryOrType = obj.optString("countryOrType", "Custom"),
                        widthMm = obj.optDouble("widthMm", 35.0).toFloat(),
                        heightMm = obj.optDouble("heightMm", 45.0).toFloat(),
                        standardDpi = obj.optInt("standardDpi", 300),
                        minHeadPercent = obj.optDouble("minHeadPercent", 0.70).toFloat(),
                        maxHeadPercent = obj.optDouble("maxHeadPercent", 0.80).toFloat(),
                        description = obj.optString("description", ""),
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun saveCustomPhotoSpec(spec: CustomPhotoSpec) {
        val current = getCustomPhotoSpecs().toMutableList()
        // Replace existing with same ID if editing, or prepend if new
        val idx = current.indexOfFirst { it.id == spec.id }
        if (idx >= 0) {
            current[idx] = spec
        } else {
            current.add(0, spec)
        }
        persistPhotoSpecs(current)
    }

    fun deleteCustomPhotoSpec(id: String) {
        val current = getCustomPhotoSpecs().toMutableList()
        current.removeAll { it.id == id }
        persistPhotoSpecs(current)
    }

    private fun persistPhotoSpecs(specs: List<CustomPhotoSpec>) {
        val jsonArray = JSONArray()
        for (spec in specs) {
            val obj = JSONObject().apply {
                put("id", spec.id)
                put("title", spec.title)
                put("countryOrType", spec.countryOrType)
                put("widthMm", spec.widthMm.toDouble())
                put("heightMm", spec.heightMm.toDouble())
                put("standardDpi", spec.standardDpi)
                put("minHeadPercent", spec.minHeadPercent.toDouble())
                put("maxHeadPercent", spec.maxHeadPercent.toDouble())
                put("description", spec.description)
                put("isCustom", true)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PHOTO_SPECS, jsonArray.toString()).apply()
    }

    // ── Custom Print Paper Definitions ──────────────────────────────

    fun getCustomPapers(): List<PrintPaperDefinition> {
        val jsonStr = prefs.getString(KEY_CUSTOM_PAPER_DEFS, null) ?: return emptyList()
        val result = mutableListOf<PrintPaperDefinition>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    PrintPaperDefinition(
                        id = obj.optString("id", "custom_paper_${System.currentTimeMillis()}_$i"),
                        title = obj.optString("title", "Custom Paper"),
                        dimensionInches = obj.optString("dimensionInches", ""),
                        widthInches = obj.optDouble("widthInches", 4.0).toFloat(),
                        heightInches = obj.optDouble("heightInches", 6.0).toFloat(),
                        widthMm = obj.optDouble("widthMm", 101.6).toFloat(),
                        heightMm = obj.optDouble("heightMm", 152.4).toFloat(),
                        defaultGridRows = obj.optInt("defaultGridRows", 1),
                        defaultGridCols = obj.optInt("defaultGridCols", 1),
                        standardDpi = obj.optInt("standardDpi", 300),
                        description = obj.optString("description", ""),
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun saveCustomPaper(paper: PrintPaperDefinition) {
        val current = getCustomPapers().toMutableList()
        val idx = current.indexOfFirst { it.id == paper.id }
        if (idx >= 0) {
            current[idx] = paper
        } else {
            current.add(0, paper)
        }
        persistPapers(current)
    }

    fun deleteCustomPaper(id: String) {
        val current = getCustomPapers().toMutableList()
        current.removeAll { it.id == id }
        persistPapers(current)
    }

    private fun persistPapers(papers: List<PrintPaperDefinition>) {
        val jsonArray = JSONArray()
        for (paper in papers) {
            val obj = JSONObject().apply {
                put("id", paper.id)
                put("title", paper.title)
                put("dimensionInches", paper.dimensionInches)
                put("widthInches", paper.widthInches.toDouble())
                put("heightInches", paper.heightInches.toDouble())
                put("widthMm", paper.widthMm.toDouble())
                put("heightMm", paper.heightMm.toDouble())
                put("defaultGridRows", paper.defaultGridRows)
                put("defaultGridCols", paper.defaultGridCols)
                put("standardDpi", paper.standardDpi)
                put("description", paper.description)
                put("isCustom", true)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PAPER_DEFS, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "gemid_custom_presets"
        private const val KEY_CUSTOM_PHOTO_SPECS = "custom_photo_specs_json"
        private const val KEY_CUSTOM_PAPER_DEFS = "custom_paper_defs_json"
    }
}
