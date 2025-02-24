package ru.otus.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SampleViewModel : ViewModel() {
    fun globalScopeViolation() {
        GlobalScope.async {

        }
    }

    suspend fun coroutineLaunchInSuspendViolation() {
        viewModelScope.launch {

        }
    }
}
