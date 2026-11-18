package com.lensstock.app

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class LensStockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Explicit offline persistence (on by default, set explicitly for clarity).
        Firebase.firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {})
        }
    }
}
