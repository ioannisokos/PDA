package com.example.pda

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class GeneralTotalViewModel : ViewModel() {
    private val _generalTotal = MutableLiveData<Double>()
    val generalTotal: LiveData<Double> get() = _generalTotal
    private var firestoreListener: ListenerRegistration? = null

    // Track if the update is coming from a clear operation
    private var isClearing = false

    init {
        fetchGeneralTotal()
        setupFirestoreListener()
    }

    private fun fetchGeneralTotal() {
        FirebaseFirestore.getInstance().collection("pda").document("cafe")
            .get()
            .addOnSuccessListener { document ->
                val existingTotal = document.getDouble("generalTotal") ?: 0.0
                // Only update if not zero or if it's a clear operation
                if (existingTotal != 0.0 || isClearing) {
                    _generalTotal.value = existingTotal
                }
                isClearing = false // Reset the flag
            }
            .addOnFailureListener {
                Log.e("GeneralTotalVM", "Failed to fetch total", it)
                // Keep the current value if available
                _generalTotal.value = _generalTotal.value ?: 0.0
            }
    }

    fun updateTotal(amount: Double) {
        val currentTotal = _generalTotal.value ?: 0.0
        val newTotal = currentTotal + amount
        setTotal(newTotal)
    }

    fun clearTotal() {
        isClearing = true
        setTotal(0.0)
    }

    private fun setTotal(newValue: Double) {
        _generalTotal.value = newValue

        FirebaseFirestore.getInstance().collection("pda").document("cafe")
            .update("generalTotal", newValue)
            .addOnFailureListener { e ->
                Log.e("GeneralTotalVM", "Failed to update total", e)
                // Revert to previous value if update fails
                _generalTotal.value = _generalTotal.value
            }
    }

    private fun setupFirestoreListener() {
        firestoreListener = FirebaseFirestore.getInstance().collection("pda").document("cafe")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("GeneralTotalVM", "Listener error", it)
                    return@addSnapshotListener
                }

                snapshot?.getDouble("generalTotal")?.let { remoteTotal ->
                    // Only update if change didn't originate from us
                    if (remoteTotal != _generalTotal.value) {
                        _generalTotal.value = remoteTotal
                    }
                }
            }
    }
    override fun onCleared() {
        firestoreListener?.remove()
        super.onCleared()
    }
}