package com.cmder.mvvmdemo

class UserRepository {
    fun getUser(): User {
        return User("Cmder", 18)
    }
}

data class User(val name: String, val age: Int)