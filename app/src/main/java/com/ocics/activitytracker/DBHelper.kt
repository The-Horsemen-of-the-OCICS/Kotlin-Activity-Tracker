package com.ocics.activitytracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

const val DATABASE_NAME = "ActivityTrackerDB"
const val TABLE_NAME = "Activity"
const val COL_NAME = "Name"
const val COL_STARTTIME = "StartTime"

private const val SQL_CREATE_ENTRIES =
    "CREATE TABLE $TABLE_NAME (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "$COL_NAME VARCHAR(50)," +
            "$COL_STARTTIME VARCHAR(50))"

private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"

// Database Handler
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    // save data to db
    fun insertData(activity: Activity) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, activity.type)
            put(COL_STARTTIME, activity.startTime)
        }
        val newRowId = db?.insert(TABLE_NAME, null, values)
    }

    // read all data
    fun getAllData(): ArrayList<String> {
        val db = this.readableDatabase

        val sortOrder = COL_STARTTIME

        val cursor = db.query(
            TABLE_NAME,             // The table to query
            null,           // The array of columns to return (pass null to get all)
            null,           // The columns for the WHERE clause
            null,       // The values for the WHERE clause
            null,           // don't group the rows
            null,            // don't filter by row groups
            sortOrder               // The sort order
        )
        val list = ArrayList<String>()
        with(cursor) {
            while (moveToNext()) {
                val item = "${cursor.getString(1)}, ${cursor.getString(2)}"
                list.add(item)
            }
        }
        cursor.close()

        return list
    }
}