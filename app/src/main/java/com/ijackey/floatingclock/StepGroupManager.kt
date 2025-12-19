package com.ijackey.floatingclock

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class StepGroup(
    val name: String,
    val steps: List<AutoClickService.ClickStep>
)

class StepGroupManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("step_groups", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveStepGroup(name: String, steps: List<AutoClickService.ClickStep>) {
        val group = StepGroup(name, steps)
        val json = gson.toJson(group)
        prefs.edit().putString(name, json).apply()
    }
    
    fun loadStepGroup(name: String): StepGroup? {
        val json = prefs.getString(name, null) ?: return null
        return try {
            gson.fromJson(json, StepGroup::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getAllGroupNames(): List<String> {
        return prefs.all.keys.toList().sorted()
    }
    
    fun deleteStepGroup(name: String) {
        prefs.edit().remove(name).apply()
    }
    
    fun renameStepGroup(oldName: String, newName: String) {
        val group = loadStepGroup(oldName)
        if (group != null) {
            saveStepGroup(newName, group.steps)
            deleteStepGroup(oldName)
        }
    }
}