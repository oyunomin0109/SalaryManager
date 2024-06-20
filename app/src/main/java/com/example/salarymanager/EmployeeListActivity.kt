package com.example.salarymanager

import android.content.ContentValues
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.provider.BaseColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class EmployeeListActivity : AppCompatActivity(){

    //レイアウト部品の変数
    private lateinit var TextView: TextView
    private lateinit var DBTable: TableLayout
    private lateinit var addUserButton: ImageButton
    private lateinit var homeButton: ImageButton
    //データベースヘルパーの変数
    private lateinit var dbHelper: EmployeeDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_list)
        //レイアウト部品読み込み
        TextView = findViewById(R.id.textView)
        DBTable = findViewById(R.id.tableLayout)
        addUserButton = findViewById(R.id.addUserButton)
        homeButton = findViewById(R.id.homeButton)
        //データベースヘルパー
        dbHelper = EmployeeDatabaseHelper(this)

        val db = dbHelper.readableDatabase
        if (db == null) {
            TextView.setText("データベースが存在しません")
            DBTable.setVisibility(View.INVISIBLE)
            return
        }else {
            TextView.setText("従業員一覧表")
            showData()
        }
        homeButton.setOnClickListener {
            finish()
        }
        addUserButton.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun showData() {
        DBTable.removeAllViews()

        val db = dbHelper.readableDatabase

        val cursor: Cursor? = db.query(
            EmployeeDatabaseContract.DatabaseEntry.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null
        )

        if (cursor == null || cursor.count == 0) {
            TextView.setText("データが存在しません")
            DBTable.setVisibility(View.INVISIBLE);
            cursor?.close()
            return
        }

        val tableLayout = findViewById<TableLayout>(R.id.tableLayout)

        val headerRow = TableRow(this)
        val nameHeader = TextView(this)
        nameHeader.text = "従業員名"
        nameHeader.setGravity(Gravity.LEFT)
        nameHeader.setPadding(16, 16, 16, 16)
        nameHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val hireDataHeader = TextView(this)
        hireDataHeader.text = "勤務開始年月"
        hireDataHeader.setGravity(Gravity.LEFT)
        hireDataHeader.setPadding(16, 16, 16, 16)
        hireDataHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val remarkHeader = TextView(this)
        remarkHeader.text = "備考"
        remarkHeader.setGravity(Gravity.LEFT)
        remarkHeader.setPadding(16, 16, 16, 16)
        remarkHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        headerRow.addView(nameHeader)
        headerRow.addView(hireDataHeader)
        headerRow.addView(remarkHeader)

        tableLayout.addView(headerRow)

        val separator = View(this)
        separator.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            1
        )
        separator.setBackgroundColor(Color.BLACK)
        tableLayout.addView(separator)

        cursor.use {
            while (it.moveToNext()) {
                val id =
                    it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                val name =
                    it.getString(it.getColumnIndexOrThrow(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_NAME))
                val hire_data =
                    it.getString(it.getColumnIndexOrThrow(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_HIRE_DATA))
                val remark =
                    it.getString(it.getColumnIndexOrThrow(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_REMARKS))
                val tableRow = TableRow(this)
                val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
                tableRow.layoutParams = layoutParams

                val nameTextView = TextView(this)
                nameTextView.text = name
                nameTextView.setGravity(Gravity.LEFT)
                nameTextView.setPadding(16, 16, 16, 16)
                val hireDataTextView = TextView(this)
                hireDataTextView.text = hire_data
                hireDataTextView.setGravity(Gravity.LEFT)
                hireDataTextView.setPadding(16, 16, 16, 16)
                val remarkTextView = TextView(this)
                remarkTextView.text = remark
                remarkTextView.setGravity(Gravity.LEFT)
                remarkTextView.setPadding(16, 16, 16, 16)
                tableRow.addView(nameTextView)
                tableRow.addView(hireDataTextView)
                tableRow.addView(remarkTextView)

                tableRow.setOnClickListener {
                    showUserDataDialog(id, name, hire_data, remark)
                }

                tableLayout.addView(tableRow)

                val separator = View(this)
                separator.layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                separator.setBackgroundColor(Color.BLACK)
                tableLayout.addView(separator)
            }
        }
    }

    private fun showUserDataDialog(id: Long, name: String, hire_data: String, remark: String?) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Data Details")
        dialogBuilder.setMessage("ID: $id\n" +
                "従業員名: $name\n" +
                "勤務開始年月: $hire_data\n" +
                "備考: $remark\n")
        dialogBuilder.setPositiveButton("確認") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.setNegativeButton("削除") { dialog, _ ->
            deleteData(id)
            dialog.dismiss()
        }
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteData(id: Long) {
        val db = dbHelper.writableDatabase
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        db.delete(EmployeeDatabaseContract.DatabaseEntry.TABLE_NAME, selection, selectionArgs)
        Toast.makeText(this, "データを削除しました", Toast.LENGTH_SHORT).show()
        // 再表示する
        showData()
    }

    //ユーザー追加ダイアログ
    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val hireDataEditText = dialogView.findViewById<EditText>(R.id.hireDataEditText)
        val remarkEditText = dialogView.findViewById<EditText>(R.id.remarkEditText)

        AlertDialog.Builder(this)
            .setTitle("保存")
            .setView(dialogView)
            .setPositiveButton("保存する") { _, _ ->
                val name = nameEditText.text.toString()
                val hire_data = hireDataEditText.text.toString()
                val remark = remarkEditText.text.toString()
                AddUserDatabase(name, hire_data, remark)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
    //ユーザー追加のデータベースに保存する関数
    private fun AddUserDatabase(name: String, startwork: String, remark: String?) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_NAME, name)
            put(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_HIRE_DATA, startwork)
            put(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_REMARKS, remark)
        }

        val newRowId = db?.insert(EmployeeDatabaseContract.DatabaseEntry.TABLE_NAME, null, values)
        if (newRowId != null && newRowId != -1L) {
            Toast.makeText(this, "データを保存しました！", Toast.LENGTH_SHORT).show()
            showData()
        } else {
            Toast.makeText(this, "データを保存できませんでした", Toast.LENGTH_SHORT).show()
        }
    }
}