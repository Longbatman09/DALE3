package com.example.dale

data class AppGroup(
    val id: String = "",
    val groupName: String = "",
    val app1PackageName: String = "",
    val app1Name: String = "",
    val app2PackageName: String = "",
    val app2Name: String = "",
    val isLocked: Boolean = false,
    val app1LockPin: String = "",
    val app2LockPin: String = "",
    val app1LockType: String = "PIN",
    val app2LockType: String = "PIN",
    val createdAt: Long = System.currentTimeMillis()
)
