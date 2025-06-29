package ru.otus.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SampleViewModel : ViewModel() {
    fun globalScopeViolation() {
        GlobalScope.async {

        }
    }

    suspend fun coroutineLaunchInSuspendViolation() {
        val globalScope = GlobalScope
        globalScope.launch {  }
        GlobalScope.async {

        }
        coroutineScope {

        }
        viewModelScope.launch {
            println()
        }
    }
}
