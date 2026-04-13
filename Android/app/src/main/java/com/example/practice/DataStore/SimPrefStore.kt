package com.example.practice.DataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "sim_prefs")

object SimPrefStore {
    private val SELECTED_SIM_KEY = intPreferencesKey("selected_sim_id")

    suspend fun saveSimId(context: Context, id: Int) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_SIM_KEY] = id
        }
    }

    suspend fun getSimId(context: Context): Int? {
        val prefs = context.dataStore.data.first()
        return prefs[SELECTED_SIM_KEY]
    }
}
