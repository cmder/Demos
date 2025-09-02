package com.cmder.mvvmdemo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _user = MutableStateFlow(User("未知", 0))
    val user: StateFlow<User> = _user

    fun loadUser() {
        _user.value = repository.getUser()
    }
}