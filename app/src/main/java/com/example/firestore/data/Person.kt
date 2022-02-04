package com.example.firestore.data

data class Person (
    //Empty by default, because i don't have other constructor for this example
    val firstName: String = "",
    val lastName: String = "",
    val age: Int = -1
)