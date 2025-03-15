
package ru.otus.example.coroutineinsuspendfun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

interface CoScope : CoroutineScope {
    suspend fun stuff() {}
}

class CoroutineInSuspendFunTestCase : ViewModel() {

    suspend fun case1() {
        viewModelScope.launch {
            delay(1000)
            println("Hello viewModelScope.launch")
        }
    }

    suspend fun case2() {
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            println("Hello CoroutineScope().launch")
        }
    }

    suspend fun case3(scope : CoroutineScope) {
        scope.launch {
            delay(1000)
            println("Hello scope.launch")
        }
    }

    suspend fun case4() {
        coroutineScope {
            launch {
                delay(1000)
                println("Hello coroutineScope.launch")
            }
        }
    }

    suspend fun case5() {
        supervisorScope {
            launch {
                delay(1000)
                println("Hello supervisorScope.launch")

            }
            async {
                delay(1000)
                println("Hello supervisorScope.async")
            }
        }
    }

    suspend fun case6(scope : CoScope) {
        scope.launch {
            delay(1000)
            println("Hello scope.launch")
        }
    }
}
