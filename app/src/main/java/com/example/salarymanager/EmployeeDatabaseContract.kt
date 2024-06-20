package com.example.salarymanager

import android.provider.BaseColumns

object EmployeeDatabaseContract {
    object DatabaseEntry : BaseColumns {
        const val TABLE_NAME = "employee"
        const val EMPLOYEE_NAME = "name"
        const val EMPLOYEE_HIRE_DATA = "data"
        const val EMPLOYEE_REMARKS = "remarks"
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${DatabaseEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${DatabaseEntry.EMPLOYEE_NAME} TEXT," +
            "${DatabaseEntry.EMPLOYEE_HIRE_DATA} TEXT," +
            "${DatabaseEntry.EMPLOYEE_REMARKS} TEXT)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${DatabaseEntry.TABLE_NAME}"
}