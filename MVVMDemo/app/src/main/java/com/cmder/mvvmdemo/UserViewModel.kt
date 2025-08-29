package com.cmder.mvvmdemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UserViewModel: ViewModel() {
    private val repository = UserRepository()
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private var job: Job? = null

    fun startAutoUpdate() {
        job = CoroutineScope(Dispatchers.Main).launch {
            while(isActive) {
                _user.value = repository.getUser()
                delay(2000)
            }
        }
    }

    fun stopAutoUpdate() {
        job?.cancel()
    }

    fun loadUser() {
        _user.value = repository.getUser()
    }
}