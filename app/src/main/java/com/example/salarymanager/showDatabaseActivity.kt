package com.example.salarymanager

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException

class showDatabaseActivity : AppCompatActivity() {

    private lateinit var dbHelper: SalaryDatabaseHelper
    private lateinit var infoTextView: TextView
    private lateinit var dataTable: TableLayout
    private lateinit var yearSpinner: Spinner
    private lateinit var monthSpinner: Spinner
    private lateinit var viewButton: Button
    private lateinit var excelButton: ImageButton
    private lateinit var homeButton: ImageButton

    companion object {
        private const val CREATE_FILE_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database)
        initViews()
        setupDatabase()

        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedYear = parent?.getItemAtPosition(position).toString()
                if (selectedYear != "All") {
                    setupMonthSpinner(selectedYear.toInt())
                    monthSpinner.visibility = View.VISIBLE
                } else {
                    monthSpinner.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewButton.setOnClickListener {
            val selectedYear = yearSpinner.selectedItem.toString()
            val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null
            dataTable.visibility = View.VISIBLE
            infoTextView.visibility = View.INVISIBLE
            showData(selectedYear, selectedMonth)
        }

        excelButton.setOnClickListener {
            saveAsExcel()
        }

        homeButton.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        infoTextView = findViewById(R.id.textView)
        dataTable = findViewById(R.id.tableLayout)
        yearSpinner = findViewById(R.id.year_spinner)
        monthSpinner = findViewById(R.id.month_spinner)
        viewButton = findViewById(R.id.view_button)
        excelButton = findViewById(R.id.excelButton)
        homeButton = findViewById(R.id.homeButton)
    }

    private fun setupDatabase() {
        dbHelper = SalaryDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        if (db == null) {
            infoTextView.text = "データベースが存在しません"
            setViewVisibility(View.INVISIBLE, dataTable, yearSpinner, monthSpinner, viewButton)
        } else {
            infoTextView.text = "閲覧したいデータを選択してください"
            setViewVisibility(View.INVISIBLE, dataTable, monthSpinner)
            setupYearSpinner()
        }
    }

    private fun setupYearSpinner() {
        val db = dbHelper.readableDatabase
        val years = mutableListOf("All")
        db.rawQuery("SELECT DISTINCT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME}", null).use { cursor ->
            while (cursor.moveToNext()) {
                years.add(cursor.getInt(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR)).toString())
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = adapter
    }

    private fun setupMonthSpinner(selectedYear: Int) {
        val db = dbHelper.readableDatabase
        val months = mutableListOf("All")
        db.rawQuery("SELECT DISTINCT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME} WHERE ${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?", arrayOf(selectedYear.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                months.add(cursor.getInt(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH)).toString())
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter
    }

    private fun showData(selectedYear: String, selectedMonth: String?) {
        dataTable.removeAllViews()
        val db = dbHelper.readableDatabase
        val (selection, selectionArgs) = if (selectedYear == "All") {
            Pair(null, null)
        } else {
            if (selectedMonth != null && selectedMonth != "All") {
                Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ? AND ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} = ?", arrayOf(selectedYear, selectedMonth))
            } else {
                Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?", arrayOf(selectedYear))
            }
        }

        db.query(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null).use { cursor ->
            if (cursor == null || cursor.count == 0) {
                infoTextView.text = "データが存在しません"
                dataTable.visibility = View.INVISIBLE
                return
            }
            addTableHeader()
            while (cursor.moveToNext()) {
                val year = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR))
                val month = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME))
                val totalSalary = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))
                val baseSalary = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY))
                val baseHours = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS))
                val baseTimes = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES))
                val baseHourlyWage = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE))
                val baseWorkingCount = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT))
                val holidaySalary = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY))
                val holidayHours = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS))
                val holidayTimes = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES))
                val holidayHourlyWage = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE))
                val holidayWorkingCount = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT))
                addTableRow(year, month, name, totalSalary, baseSalary, baseHours, baseTimes, baseHourlyWage, baseWorkingCount, holidaySalary, holidayHours, holidayTimes, holidayHourlyWage, holidayWorkingCount)
            }
            excelButton.visibility = View.VISIBLE
        }
    }

    private fun addTableHeader() {
        val headerRow = TableRow(this)
        val headers = listOf("年", "月", "名前", "総給料", "平日給", "平日時", "平日分", "平日時給", "平日勤務回", "土休差額", "土休時", "土休分", "土休時給差額", "土休勤務回")
        headers.forEach { header ->
            val headerView = TextView(this).apply {
                text = header
                gravity = Gravity.LEFT
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.rgb(224, 224, 224))
            }
            headerRow.addView(headerView)
        }
        dataTable.addView(headerRow)
        addSeparator()
    }

    private fun addTableRow(vararg values: String) {
        val tableRow = TableRow(this)
        values.forEach { value ->
            val textView = TextView(this).apply {
                text = value
                gravity = Gravity.LEFT
                setPadding(16, 16, 16, 16)
            }
            tableRow.addView(textView)
        }
        tableRow.setOnClickListener {
            showDataDialog(values[0].toLong(), *values)
        }
        dataTable.addView(tableRow)
        addSeparator()
    }

    private fun addSeparator() {
        val separator = View(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.BLACK)
        }
        dataTable.addView(separator)
    }

    private fun showDataDialog(id: Long, vararg values: String) {
        val details = "ID: $id\n" +
                "従業員名: ${values[2]}\n" +
                "年: ${values[0]}\n" +
                "月: ${values[1]}\n" +
                "給料額: ${values[3]}\n" +
                "基本(平日)給料額: ${values[4]}\n" +
                "基本(平日)勤務時間(時): ${values[5]}\n" +
                "基本(平日)勤務時間(分): ${values[6]}\n" +
                "基本(平日)時給: ${values[7]}\n" +
                "合計勤務回数: ${values[8]}\n" +
                "土休祝差額: ${values[9]}\n" +
                "土休祝勤務時間(時): ${values[10]}\n" +
                "土休祝勤務時間(分): ${values[11]}\n" +
                "土休祝差額時給: ${values[12]}\n" +
                "土休祝差額時給: ${values[13]}\n"

        AlertDialog.Builder(this)
            .setTitle("詳細データ")
            .setMessage(details)
            .setPositiveButton("確認") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("削除") { dialog, _ -> deleteData(id); dialog.dismiss() }
            .create()
            .show()
    }

    private fun deleteData(id: Long) {
        val db = dbHelper.writableDatabase
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        db.delete(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, selection, selectionArgs)
        Toast.makeText(this, "データを削除しました", Toast.LENGTH_SHORT).show()
        showData(yearSpinner.selectedItem.toString(), if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null)
    }

    private fun saveAsExcel() {
        val selectedYear = yearSpinner.selectedItem.toString()
        val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) {
            monthSpinner.selectedItem.toString().toIntOrNull()
        } else {
            null
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "data.xlsx")
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE, Bundle().apply {
            putString("year", selectedYear)
            putInt("month", selectedMonth ?: -1)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.let { uri ->
                saveExcelToUri(uri, yearSpinner.selectedItem.toString(), if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null)
            }
        }
    }

    private fun saveExcelToUri(uri: Uri, selectedYear: String, selectedMonth: String?) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook: Workbook = XSSFWorkbook()
                val sheet: Sheet = workbook.createSheet("データ")

                val db = dbHelper.readableDatabase

                // ヘッダー行なし

                // データ行のタイトル
                val headers = arrayOf(
                    "従業員名", "年/月", "基本給料", "基本給料", "勤務回数", "時間", "分",
                    "基本時給", "土休差額", "土休差額", "勤務回数", "時間", "分",
                    "土休日差額時給", "合計給料額"
                )

                var rowIndex = 0
                headers.forEachIndexed { index, header ->
                    val row = sheet.createRow(rowIndex)
                    row.createCell(0).setCellValue(header)
                    rowIndex++
                }

                // データ行を設定
                val (selection, selectionArgs) = if (selectedYear == "All") {
                    Pair(null, null)
                } else {
                    if (selectedMonth != null && selectedMonth != "All") {
                        Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ? AND ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} = ?",
                            arrayOf(selectedYear, selectedMonth))
                    } else {
                        Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?",
                            arrayOf(selectedYear))
                    }
                }

                db.query(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        var columnIndex = 1 // データ格納開始列
                        do {
                            rowIndex = 0
                            val rowValues = arrayOf(
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME)),
                                "${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR))}/${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH))}",
                                "基本給料",
                                "￥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY))}",
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES)),
                                "￥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE))}",
                                "土休差額",
                                "￥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY))}",
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES)),
                                "￥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE))}",
                                "￥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))}"
                            )

                            rowValues.forEach { value ->
                                val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                                val cell = row.createCell(columnIndex)
                                cell.setCellValue(value)

                                // 罫線のスタイルを追加
                                val cellStyle = workbook.createCellStyle()
                                cellStyle.borderTop = BorderStyle.THIN
                                cellStyle.borderBottom = BorderStyle.THIN
                                cellStyle.borderLeft = BorderStyle.THIN
                                cellStyle.borderRight = BorderStyle.THIN
                                cell.cellStyle = cellStyle

                                rowIndex++
                            }

                            columnIndex++
                        } while (cursor.moveToNext())
                    }
                }

                for (i in 0..sheet.getRow(0).lastCellNum) {
                    sheet.setColumnWidth(i, 3000) // 列幅
                }
                workbook.write(outputStream)
                workbook.close()
                Toast.makeText(this, "Excelとして保存しました", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("Excel", "Error writing Excel", e)
            Toast.makeText(this, "保存ができませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setViewVisibility(visibility: Int, vararg views: View) {
        views.forEach { it.visibility = visibility }
    }
}