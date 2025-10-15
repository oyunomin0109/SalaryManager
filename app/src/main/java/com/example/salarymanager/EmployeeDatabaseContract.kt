package com.example.salarymanager

import android.provider.BaseColumns

object EmployeeDatabaseContract {
    object DatabaseEntry : BaseColumns {
        const val TABLE_NAME = "employee"
        const val EMPLOYEE_NAME = "name"
        const val EMPLOYEE_HIRE_DATA = "data"
        const val EMPLOYEE_REMARKS = "remarks"
        const val WEEKDAYS_HOURLY_WAGE = "weekdays_hourly_wage" // 平日時給
        const val HOLIDAY_HOURLY_WAGE = "holiday_hourly_wage" // 休日時給
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${DatabaseEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${DatabaseEntry.EMPLOYEE_NAME} TEXT," +
                "${DatabaseEntry.EMPLOYEE_HIRE_DATA} TEXT," +
                "${DatabaseEntry.EMPLOYEE_REMARKS} TEXT," +
                "${DatabaseEntry.WEEKDAYS_HOURLY_WAGE} INTEGER DEFAULT 1300," + // デフォルト値を設定
                "${DatabaseEntry.HOLIDAY_HOURLY_WAGE} INTEGER DEFAULT 50)" // デフォルト値を設定

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${DatabaseEntry.TABLE_NAME}"

    const val SQL_UPGRADE_ENTRIES =
        "ALTER TABLE ${DatabaseEntry.TABLE_NAME} ADD COLUMN ${DatabaseEntry.WEEKDAYS_HOURLY_WAGE} INTEGER DEFAULT 1300;" +
                "ALTER TABLE ${DatabaseEntry.TABLE_NAME} ADD COLUMN ${DatabaseEntry.HOLIDAY_HOURLY_WAGE} INTEGER DEFAULT 50;"
}