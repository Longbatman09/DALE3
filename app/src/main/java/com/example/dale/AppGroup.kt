package com.example.dale

data class AppGroup(
    val id: String = "",
    val groupName: String = "",
    val app1PackageName: String = "",
    val app1Name: String = "",
    val app2PackageName: String = "",
    val app2Name: String = "",
    val isLocked: Boolean = false,
    val lockPin: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

