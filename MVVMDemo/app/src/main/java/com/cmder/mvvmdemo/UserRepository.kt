package com.cmder.mvvmdemo

import kotlin.random.Random
import kotlin.random.nextInt

class UserRepository {
    fun getUser(): User {
        val age = Random.nextInt(18, 30)
        return User("Cmder", age)
    }
}

data class User(val name: String, val age: Int)