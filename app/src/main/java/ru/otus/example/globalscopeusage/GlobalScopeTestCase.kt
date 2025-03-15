@file:OptIn(DelicateCoroutinesApi::class)

package ru.otus.example.globalscopeusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {

    fun case1() {
        GlobalScope.launch {
            delay(1000)
            println("Hello GlobalScope (launch)")
        }
        GlobalScope.actor<String> {
            delay(1000)
            println("Hello GlobalScope (actor)")
        }
    }

    fun case2() {
        viewModelScope.launch {
            val deferred = GlobalScope.async {
                delay(1000)
                "Hello GlobalScope (async)"
            }
            println(deferred.await())
        }
    }

    fun case3() {
        scope.launch {
            delay(1000)
            println("Hello scope (launch)")
        }
    }
}
