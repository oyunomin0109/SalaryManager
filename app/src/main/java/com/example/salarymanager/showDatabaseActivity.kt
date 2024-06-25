package com.example.salarymanager

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter

class showDatabaseActivity : AppCompatActivity(){

    private lateinit var dbHelper: SalaryDatabaseHelper
    private lateinit var TextView: TextView
    private lateinit var DBTable: TableLayout
    private lateinit var yearSpinner: Spinner
    private lateinit var monthSpinner: Spinner
    private lateinit var viewButton: Button
    private lateinit var csvButton: ImageButton
    private lateinit var homeButton: ImageButton

    companion object {
        private const val CREATE_FILE_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database)

        TextView = findViewById(R.id.textView)
        dbHelper = SalaryDatabaseHelper(this)
        DBTable = findViewById(R.id.tableLayout)
        yearSpinner = findViewById(R.id.year_spinner)
        monthSpinner = findViewById(R.id.month_spinner)
        viewButton = findViewById(R.id.view_button)
        csvButton = findViewById(R.id.csvButton)
        homeButton = findViewById(R.id.homeButton)

        val db = dbHelper.readableDatabase
        if (db == null) {
            TextView.text = "データベースが存在しません"
            DBTable.setVisibility(View.INVISIBLE)
            yearSpinner.setVisibility(View.INVISIBLE)
            monthSpinner.setVisibility(View.INVISIBLE)
            viewButton.setVisibility(View.INVISIBLE)
            return
        }else {
            TextView.text = "閲覧したいデータを選択してください"
            DBTable.setVisibility(View.INVISIBLE)
            monthSpinner.setVisibility(View.INVISIBLE)
        }

        setupYearSpinner()
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
            DBTable.setVisibility(View.VISIBLE)
            TextView.setVisibility(View.INVISIBLE)
            showData(selectedYear, selectedMonth)
        }

        csvButton.setVisibility(View.INVISIBLE)
        csvButton.setOnClickListener {
            saveAsCSV()
        }
        homeButton.setOnClickListener {
            finish()
        }
    }

    private fun setupYearSpinner() {
        val db = dbHelper.readableDatabase
        val years = mutableListOf<String>()
        years.add("All")
        val cursor: Cursor? = db?.rawQuery("SELECT DISTINCT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME}", null)
        cursor?.use {
            while (it.moveToNext()) {
                val year = it.getInt(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR)).toString()
                years.add(year)
            }
        }
        cursor?.close()

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = adapter
    }

    private fun setupMonthSpinner(selectedYear: Int) {
        val db = dbHelper.readableDatabase
        val months = mutableListOf<String>()
        months.add("All")
        val cursor: Cursor? = db?.rawQuery("SELECT DISTINCT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME} WHERE ${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?", arrayOf(selectedYear.toString()))
        cursor?.use {
            while (it.moveToNext()) {
                val month = it.getInt(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH)).toString()
                months.add(month)
            }
        }
        cursor?.close()

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter
    }

    private fun showData(selectedYear: String, selectedMonth: String?) {
        DBTable.removeAllViews()

        val db = dbHelper.readableDatabase
        val selection: String?
        val selectionArgs: Array<String>?

        if (selectedYear == "All") {
            selection = null
            selectionArgs = null
        } else {
            if (selectedMonth != null && selectedMonth != "All") {
                selection = "${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ? AND ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} = ?"
                selectionArgs = arrayOf(selectedYear, selectedMonth)
            } else {
                selection = "${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?"
                selectionArgs = arrayOf(selectedYear)
            }
        }

        val cursor: Cursor? = db.query(
            SalaryDatabaseContract.DatabaseEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor == null || cursor.count == 0) {
            TextView.text = "データが存在しません"
            DBTable.setVisibility(View.INVISIBLE);
            cursor?.close()
            return
        }

        val tableLayout = findViewById<TableLayout>(R.id.tableLayout)

        val headerRow = TableRow(this)
        val yearHeader = TextView(this)
        yearHeader.text = "年"
        yearHeader.setGravity(Gravity.LEFT)
        yearHeader.setPadding(16, 16, 16, 16)
        yearHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val monthHeader = TextView(this)
        monthHeader.text = "月"
        monthHeader.setGravity(Gravity.LEFT)
        monthHeader.setPadding(16, 16, 16, 16)
        monthHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val nameHeader = TextView(this)
        nameHeader.text = "名前"
        nameHeader.setGravity(Gravity.LEFT)
        nameHeader.setPadding(16, 16, 16, 16)
        nameHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val totalsalaryHeader = TextView(this)
        totalsalaryHeader.text = "総給料"
        totalsalaryHeader.setGravity(Gravity.LEFT)
        totalsalaryHeader.setPadding(16, 16, 16, 16)
        totalsalaryHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val basesalaryHeader = TextView(this)
        basesalaryHeader.text = "平日給"
        basesalaryHeader.setGravity(Gravity.LEFT)
        basesalaryHeader.setPadding(16, 16, 16, 16)
        basesalaryHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val basehourHeader = TextView(this)
        basehourHeader.text = "平日時"
        basehourHeader.setGravity(Gravity.LEFT)
        basehourHeader.setPadding(16, 16, 16, 16)
        basehourHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val basetimeHeader = TextView(this)
        basetimeHeader.text = "平日分"
        basetimeHeader.setGravity(Gravity.LEFT)
        basetimeHeader.setPadding(16, 16, 16, 16)
        basetimeHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val basehourlywageHeader = TextView(this)
        basehourlywageHeader.text = "平日時給"
        basehourlywageHeader.setGravity(Gravity.LEFT)
        basehourlywageHeader.setPadding(16, 16, 16, 16)
        basehourlywageHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val baseworkingcountHeader = TextView(this)
        baseworkingcountHeader.text = "平日勤務回"
        baseworkingcountHeader.setGravity(Gravity.LEFT)
        baseworkingcountHeader.setPadding(16, 16, 16, 16)
        baseworkingcountHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val holidaysalaryHeader = TextView(this)
        holidaysalaryHeader.text = "土休差額"
        holidaysalaryHeader.setGravity(Gravity.LEFT)
        holidaysalaryHeader.setPadding(16, 16, 16, 16)
        holidaysalaryHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val holidayhourHeader = TextView(this)
        holidayhourHeader.text = "土休時"
        holidayhourHeader.setGravity(Gravity.LEFT)
        holidayhourHeader.setPadding(16, 16, 16, 16)
        holidayhourHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val holidaytimeHeader = TextView(this)
        holidaytimeHeader.text = "土休分"
        holidaytimeHeader.setGravity(Gravity.LEFT)
        holidaytimeHeader.setPadding(16, 16, 16, 16)
        holidaytimeHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val holidayhourlywageHeader = TextView(this)
        holidayhourlywageHeader.text = "土休時給差額"
        holidayhourlywageHeader.setGravity(Gravity.LEFT)
        holidayhourlywageHeader.setPadding(16, 16, 16, 16)
        holidayhourlywageHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        val holidayworkingcountHeader = TextView(this)
        holidayworkingcountHeader.text = "土休勤務回"
        holidayworkingcountHeader.setGravity(Gravity.LEFT)
        holidayworkingcountHeader.setPadding(16, 16, 16, 16)
        holidayworkingcountHeader.setBackgroundColor(Color.rgb(224, 224, 224))
        headerRow.addView(yearHeader)
        headerRow.addView(monthHeader)
        headerRow.addView(nameHeader)
        headerRow.addView(totalsalaryHeader)
        headerRow.addView(basesalaryHeader)
        headerRow.addView(basehourHeader)
        headerRow.addView(basetimeHeader)
        headerRow.addView(basehourlywageHeader)
        headerRow.addView(baseworkingcountHeader)
        headerRow.addView(holidaysalaryHeader)
        headerRow.addView(holidayhourHeader)
        headerRow.addView(holidaytimeHeader)
        headerRow.addView(holidayhourlywageHeader)
        headerRow.addView(holidayworkingcountHeader)

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
                val year =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR))
                val month =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH))
                val name =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME))
                val totalsalary =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))
                val basesalary =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY))
                val basehours =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS))
                val basetimes =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES))
                val basehourlywage =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE))
                val baseworkingcount =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT))
                val holidaysalary =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY))
                val holidayhours =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS))
                val holidaytimes =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES))
                val holidayhourlywage =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE))
                val holidayworkingcount =
                    it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT))
                val tableRow = TableRow(this)
                val layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
                tableRow.layoutParams = layoutParams

                val yearTextView = TextView(this)
                yearTextView.text = year
                yearTextView.setGravity(Gravity.LEFT)
                yearTextView.setPadding(16, 16, 16, 16)
                val monthTextView = TextView(this)
                monthTextView.text = month
                monthTextView.setGravity(Gravity.LEFT)
                monthTextView.setPadding(16, 16, 16, 16)
                val nameTextView = TextView(this)
                nameTextView.text = name
                nameTextView.setGravity(Gravity.LEFT)
                nameTextView.setPadding(16, 16, 16, 16)
                val TotalSalaryTextView = TextView(this)
                TotalSalaryTextView.text = totalsalary
                TotalSalaryTextView.setGravity(Gravity.LEFT)
                TotalSalaryTextView.setPadding(16, 16, 16, 16)
                val BaseSalaryTextView = TextView(this)
                BaseSalaryTextView.text = basesalary
                BaseSalaryTextView.setGravity(Gravity.LEFT)
                BaseSalaryTextView.setPadding(16, 16, 16, 16)
                val BaseHoursTextView = TextView(this)
                BaseHoursTextView.text = basehours
                BaseHoursTextView.setGravity(Gravity.LEFT)
                BaseHoursTextView.setPadding(16, 16, 16, 16)
                val BaseTimesTextView = TextView(this)
                BaseTimesTextView.text = basetimes
                BaseTimesTextView.setGravity(Gravity.LEFT)
                BaseTimesTextView.setPadding(16, 16, 16, 16)
                val BaseHourlyWageTextView = TextView(this)
                BaseHourlyWageTextView.text = basehourlywage
                BaseHourlyWageTextView.setGravity(Gravity.LEFT)
                BaseHourlyWageTextView.setPadding(16, 16, 16, 16)
                val BaseWorkingCountTextView = TextView(this)
                BaseWorkingCountTextView.text = baseworkingcount
                BaseWorkingCountTextView.setGravity(Gravity.LEFT)
                BaseWorkingCountTextView.setPadding(16, 16, 16, 16)
                val HolidaySalaryTextView = TextView(this)
                HolidaySalaryTextView.text = holidaysalary
                HolidaySalaryTextView.setGravity(Gravity.LEFT)
                HolidaySalaryTextView.setPadding(16, 16, 16, 16)
                val HolidayHoursTextView = TextView(this)
                HolidayHoursTextView.text = holidayhours
                HolidayHoursTextView.setGravity(Gravity.LEFT)
                HolidayHoursTextView.setPadding(16, 16, 16, 16)
                val HolidayTimesTextView = TextView(this)
                HolidayTimesTextView.text = holidaytimes
                HolidayTimesTextView.setGravity(Gravity.LEFT)
                HolidayTimesTextView.setPadding(16, 16, 16, 16)
                val HolidayHourlyWageTextView = TextView(this)
                HolidayHourlyWageTextView.text = holidayhourlywage
                HolidayHourlyWageTextView.setGravity(Gravity.LEFT)
                HolidayHourlyWageTextView.setPadding(16, 16, 16, 16)
                val HolidayWorkingCountTextView = TextView(this)
                HolidayWorkingCountTextView.text = holidayworkingcount
                HolidayWorkingCountTextView.setGravity(Gravity.LEFT)
                HolidayWorkingCountTextView.setPadding(16, 16, 16, 16)

                tableRow.addView(yearTextView)
                tableRow.addView(monthTextView)
                tableRow.addView(nameTextView)
                tableRow.addView(TotalSalaryTextView)
                tableRow.addView(BaseSalaryTextView)
                tableRow.addView(BaseHoursTextView)
                tableRow.addView(BaseTimesTextView)
                tableRow.addView(BaseHourlyWageTextView)
                tableRow.addView(BaseWorkingCountTextView)
                tableRow.addView(HolidaySalaryTextView)
                tableRow.addView(HolidayHoursTextView)
                tableRow.addView(HolidayTimesTextView)
                tableRow.addView(HolidayHourlyWageTextView)
                tableRow.addView(HolidayWorkingCountTextView)

                tableRow.setOnClickListener {
                    showDataDialog(id, name, year, month, totalsalary, basesalary, basehours, basetimes, basehourlywage, baseworkingcount, holidaysalary, holidayhours, holidaytimes, holidayhourlywage, holidayworkingcount)
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
        csvButton.setVisibility(View.VISIBLE)
    }

    private fun showDataDialog(id: Long, name: String?, year: String?, month: String?, totalsalary: String?, basesalary: String?, basehours: String?, basetimes: String?, basehourlywage: String?, baseworkingcount: String?, holidaysalary: String?, holidayhours: String?, holidaytimes: String?, holidayhourlywage: String?, holidayworkingcount: String?, ) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("詳細データ")
        dialogBuilder.setMessage("ID: $id\n" +
                "従業員名: $name\n" +
                "年: $year\n" +
                "月: $month\n" +
                "給料額: $totalsalary\n" +
                "基本(平日)給料額: $basesalary\n" +
                "基本(平日)勤務時間(時): $basehours\n" +
                "基本(平日)勤務時間(分): $basetimes\n" +
                "基本(平日)時給: $basehourlywage\n" +
                "合計勤務回数: $baseworkingcount\n" +
                "土休祝差額: + $holidaysalary\n" +
                "土休祝勤務時間(時): $holidayhours\n" +
                "土休祝勤務時間(分): $holidaytimes\n" +
                "土休祝差額時給: $holidayhourlywage\n" +
                "土休祝差額時給: $holidayworkingcount\n")
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
        db.delete(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, selection, selectionArgs)
        Toast.makeText(this, "データを削除しました", Toast.LENGTH_SHORT).show()
        // 再表示する
        val selectedYear = yearSpinner.selectedItem.toString()
        val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null
        showData(selectedYear, selectedMonth)
    }

    private fun saveAsCSV() {
        val selectedYear = yearSpinner.selectedItem.toString()
        val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) {
            val monthString = monthSpinner.selectedItem.toString()
            if (monthString != "All") monthString.toInt() else null
        } else {
            null
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "data.csv")
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE, Bundle().apply {
            putString("year", selectedYear)
            putInt("month", selectedMonth ?: -1) // デフォルト値を設定
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val selectedYear = yearSpinner.selectedItem.toString()
                val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null
                saveCSVToUri(uri, selectedYear, selectedMonth)
            }
        }
    }

    private fun saveCSVToUri(uri: Uri, selectedYear: String, selectedMonth: String?) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = BufferedWriter(OutputStreamWriter(outputStream))
                val db = dbHelper.readableDatabase

                val selection: String?
                val selectionArgs: Array<String>?

                if (selectedYear == "All") {
                    selection = null
                    selectionArgs = null
                } else {
                    if (selectedMonth != null && selectedMonth != "All") {
                        selection = "${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ? AND ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} = ?"
                        selectionArgs = arrayOf(selectedYear, selectedMonth)
                    } else {
                        selection = "${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?"
                        selectionArgs = arrayOf(selectedYear)
                    }
                }

                val cursor: Cursor? = db.query(
                    SalaryDatabaseContract.DatabaseEntry.TABLE_NAME,
                    null,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
                )
                cursor.use {
                    writer.append("年,月,名前,総給料,平日給料,平日時,平日分,平日時給,土休差額,土休時,土休分,土休差額時給\n")
                    if (it != null) {
                        while (it.moveToNext()) {
                            val year = it.getInt(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR))
                            val month = it.getInt(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH))
                            val name = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME))
                            val totalsalary = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))
                            val basesalary = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY))
                            val basehours = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS))
                            val basetimes = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES))
                            val basehourlywage = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE))
                            val baseworkingcount = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT))
                            val holidaysalary = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY))
                            val holidayhours = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS))
                            val holidaytimes = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES))
                            val holidayhourlywage = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE))
                            val holidayworkingcount = it.getString(it.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT))
                            writer.append("$year,$month,$name,$totalsalary,$basesalary,$basehours,$basetimes,$basehourlywage,$baseworkingcount,$holidaysalary,$holidayhours,$holidaytimes,$holidayhourlywage,$holidayworkingcount\n")
                        }
                    }
                }
                writer.flush()
                writer.close()
                Toast.makeText(this, "CSVとして保存しました", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("CSV", "Error writing CSV", e)
            Toast.makeText(this, "保存ができませんでした", Toast.LENGTH_SHORT).show()
        }
    }

}