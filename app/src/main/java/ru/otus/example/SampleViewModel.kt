package ru.otus.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
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

        viewModelScope.launch {
        }
    }

    suspend fun goodUsage() {
        coroutineScope {
            launch {

            }
        }
    }

    fun dispatcher() {
        viewModelScope.launch {
        }

        viewModelScope.launch(CoroutineName("name")) {

        }

    }

    fun test(scope: CoroutineScope) {
        scope.launch {

        }
    }
}

