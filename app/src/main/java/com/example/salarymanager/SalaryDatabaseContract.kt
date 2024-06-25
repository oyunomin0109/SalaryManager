package com.example.salarymanager

import android.provider.BaseColumns

object SalaryDatabaseContract {
    object DatabaseEntry : BaseColumns {
        const val TABLE_NAME = "salaryDB"
        const val COLUMN_YEAR = "year"
        const val COLUMN_MONTH = "month"
        const val COLUMN_NAME = "name"
        const val COLUMN_TOTAL_SALARY = "totalsalary"
        const val COLUMN_BASE_SALARY = "basesalary"
        const val COLUMN_BASE_HOURS = "basehours"
        const val COLUMN_BASE_TIMES = "basetimes"
        const val COLUMN_BASE_HOURLY_WAGE = "basehourlywage"
        const val COLUMN_BASE_WORKING_COUNT = "baseworkingcount"
        const val COLUMN_HOLIDAY_SALARY = "holidaysalary"
        const val COLUMN_HOLIDAY_HOURS = "holidayhours"
        const val COLUMN_HOLIDAY_TIMES = "holidaytimes"
        const val COLUMN_HOLIDAY_HOURLY_WAGE = "holidayhourlywage"
        const val COLUMN_HOLIDAY_WORKING_COUNT = "holidayworkingcount"
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${DatabaseEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${DatabaseEntry.COLUMN_YEAR} INTEGER," +
                "${DatabaseEntry.COLUMN_MONTH} INTEGER," +
                "${DatabaseEntry.COLUMN_NAME} TEXT," +
                "${DatabaseEntry.COLUMN_TOTAL_SALARY} INTEGER," +
                "${DatabaseEntry.COLUMN_BASE_SALARY} INTEGER," +
                "${DatabaseEntry.COLUMN_BASE_HOURS} INTEGER," +
                "${DatabaseEntry.COLUMN_BASE_TIMES} INTEGER," +
                "${DatabaseEntry.COLUMN_BASE_HOURLY_WAGE} INTEGER," +
                "${DatabaseEntry.COLUMN_BASE_WORKING_COUNT} INTEGER," +
                "${DatabaseEntry.COLUMN_HOLIDAY_SALARY} INTEGER," +
                "${DatabaseEntry.COLUMN_HOLIDAY_HOURS} INTEGER," +
                "${DatabaseEntry.COLUMN_HOLIDAY_TIMES} INTEGER," +
                "${DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE} INTEGER," +
                "${DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT} INTEGER)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${DatabaseEntry.TABLE_NAME}"
}