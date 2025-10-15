package com.example.salarymanager

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EmployeeDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "SalaryManager.db"
        const val DATABASE_VERSION = 4 // バージョンを2に更新
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(EmployeeDatabaseContract.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db?.execSQL(EmployeeDatabaseContract.SQL_UPGRADE_ENTRIES)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}