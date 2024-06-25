package com.example.salarymanager

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    //計算式の部品
    private lateinit var inputTextView: TextView
    //右上のボタン部品
    private lateinit var optionMenuButton: ImageButton
    private lateinit var saveDBButton: ImageButton
    //計算式のスクロールビュー
    private lateinit var scrollView: ScrollView
    //結果出力画面のレイアウトとスクロールビュー
    private lateinit var scrollviewlayout: ConstraintLayout
    private lateinit var result_scrollView:ScrollView
    //データベースヘルパー
    private lateinit var salaryDBHelper: SalaryDatabaseHelper
    private lateinit var employeeDBHelper: EmployeeDatabaseHelper
    //イコール検知
    private var Resultbool: Boolean = false
    private var NumCheck: Boolean = true
    private var currentOperation: String = ""
    //合計勤務時間の変数(分)
    private var totaltimes: Int = 0
    //時給保存変数
    private var weekdays_hourly_wage_data: Int = 0
    private var holidays_hourly_wage_data: Int = 0
    private var custom_hourly_wage_data: Int = 0

    //勤務回数カウント変数
    private var working_count: Int = 0
    //結果を表示するテキスト部品変数
    private lateinit var total_time: TextView
    private lateinit var total_salary: TextView
    private lateinit var base_total_salary: TextView
    private lateinit var holiday_total_salary: TextView
    private lateinit var base_hourly_wage_text: TextView
    private lateinit var holiday_hourly_wage_text: TextView

    //保存させる給料データ
    private var user_total_salary = 0
    private var user_base_salary = 0
    private var user_holiday_salary = 0
    private var user_base_hourly_wage_data = 0
    private var user_holiday_hourly_wage_data = 0
    private var user_base_work_hours = 0
    private var user_base_work_times = 0
    private var user_holiday_work_hours = 0
    private var user_holiday_work_times = 0
    private var user_base_work_count = 0
    private var user_holiday_work_count = 0

    //平日、休日のボタンクリックしているかどうか
    private var base_salary_button_click: Boolean = false
    private var holiday_salary_button_click: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //計算式を表示する部分のスクロールを許可する
        inputTextView = findViewById(R.id.inputTextView)
        inputTextView.setHorizontalScrollBarEnabled(true)
        inputTextView.setVerticalScrollBarEnabled(true)
        //結果部分のスクロールを許可する
        scrollviewlayout = findViewById(R.id.scrollConstraint)
        scrollviewlayout.setHorizontalScrollBarEnabled(true)
        scrollviewlayout.setVerticalScrollBarEnabled(true)
        //右上のボタン
        optionMenuButton = findViewById(R.id.option_menu_button)
        saveDBButton = findViewById(R.id.save_database_button)
        //データベースヘルパー
        salaryDBHelper = SalaryDatabaseHelper(this)
        employeeDBHelper = EmployeeDatabaseHelper(this)
        //scrollviewの変数
        scrollView = findViewById(R.id.input_scroll_view)
        result_scrollView = findViewById(R.id.result_scroll_view)
        //結果を表示するTextView
        total_time = findViewById(R.id.total_time)
        total_salary = findViewById(R.id.total_salary)
        base_total_salary = findViewById(R.id.base_total_salary)
        holiday_total_salary = findViewById(R.id.holiday_total_salary)
        base_hourly_wage_text = findViewById(R.id.base_hourly_wage)
        holiday_hourly_wage_text = findViewById(R.id.holiday_hourly_wage)

        //時給情報を読み込む
        val hourly_wage_data = getSharedPreferences("salary_manager_hourly_wage_data", MODE_PRIVATE)
        weekdays_hourly_wage_data = hourly_wage_data.getInt("weekdays_hourly_wage", 1300) //平日
        holidays_hourly_wage_data = hourly_wage_data.getInt("holiday_hourly_wage", 50) //休日差分
        custom_hourly_wage_data = hourly_wage_data.getInt("custom_hourly_wage", 1310) //カスタム

        //ボタンをリストにしておく
        val buttons = listOf<Button>(
            findViewById(R.id.zero),
            findViewById(R.id.doublezero),
            findViewById(R.id.one),
            findViewById(R.id.two),
            findViewById(R.id.three),
            findViewById(R.id.four),
            findViewById(R.id.five),
            findViewById(R.id.six),
            findViewById(R.id.seven),
            findViewById(R.id.eight),
            findViewById(R.id.nine),
            findViewById(R.id.plus),
            findViewById(R.id.clear),
            findViewById(R.id.minus),
            findViewById(R.id.backspace),
            findViewById(R.id.equal),
            findViewById(R.id.weekdays),
            findViewById(R.id.holiday),
            findViewById(R.id.customsalary),
            findViewById(R.id.allclear)
        )

        optionMenuButton.setOnClickListener {
            showPopupMenu(it)
        }

        saveDBButton.setOnClickListener {
            showSaveDialog()
        }

        buttons.forEach { button ->
            button.setOnClickListener {
                onButtonClick(button.text.toString())
            }
        }

    }

    private fun onButtonClick(value: String) {
        when {
            value == "0" || value == "00" || value == "1" || value == "2" || value == "3" || value == "4" || value == "5" || value == "6" || value == "7" || value == "8" || value == "9" -> {
                if(Resultbool == false){
                    val text = inputTextView.text.toString()
                    if (text.isEmpty()) {
                        inputTextView.setText(value)
                        NumCheck = true
                    } else {
                        inputTextView.append(value)
                        NumCheck = true
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
            value == "平日" -> {
                if(Resultbool == true){
                    val result: Int = floor(totaltimes * weekdays_hourly_wage_data.toDouble() / 60.0).toInt()
                    base_total_salary.setText(result.toString() + "円")
                    base_hourly_wage_text.setText("勤務時間：" + (totaltimes / 60).toString() + ":" + (totaltimes % 60).toString() + "\n時給：" + weekdays_hourly_wage_data + "円")
                    user_base_salary = result
                    user_base_hourly_wage_data = weekdays_hourly_wage_data
                    user_base_work_hours = totaltimes / 60
                    user_base_work_times = totaltimes % 60
                    base_salary_button_click = true
                    user_base_work_count = working_count
                    if(holiday_salary_button_click){
                        user_total_salary = user_base_salary + user_holiday_salary
                        total_salary.setText(user_total_salary.toString() + "円")
                    }
                }
            }
            value == "休日差額" -> {
                if(Resultbool == true){
                    val result: Int = floor(totaltimes * holidays_hourly_wage_data.toDouble() / 60.0).toInt()
                    holiday_total_salary.setText(result.toString() + "円")
                    holiday_hourly_wage_text.setText("勤務時間：" + (totaltimes / 60).toString() + ":" + (totaltimes % 60).toString() + "\n土休差額：" + holidays_hourly_wage_data + "円")
                    user_holiday_salary = result
                    user_holiday_hourly_wage_data = holidays_hourly_wage_data
                    user_holiday_work_hours = totaltimes / 60
                    user_holiday_work_times = totaltimes % 60
                    holiday_salary_button_click = true
                    user_holiday_work_count = working_count
                    if(base_salary_button_click){
                        user_total_salary = user_base_salary + user_holiday_salary
                        total_salary.setText(user_total_salary.toString() + "円")
                    }
                }
            }
            value == "カスタム" -> {
                if(Resultbool == true){
                    val result: Int = floor(totaltimes * custom_hourly_wage_data.toDouble() / 60.0).toInt()
                    base_total_salary.setText(result.toString() + "円")
                    base_hourly_wage_text.setText("勤務時間：" + (totaltimes / 60).toString() + ":" + (totaltimes % 60).toString() + "\n時給：" + custom_hourly_wage_data + "円")
                    user_base_salary = result
                    user_base_hourly_wage_data = custom_hourly_wage_data
                    user_base_work_hours = totaltimes / 60
                    user_base_work_times = totaltimes % 60
                    base_salary_button_click = true
                    user_base_work_count = working_count
                    if(holiday_salary_button_click){
                        user_total_salary = user_base_salary + user_holiday_salary
                        total_salary.setText(user_total_salary.toString() + "円")
                    }
                }
            }
            value == "AC" -> {
                inputTextView.setText("")
                Resultbool = false
                totaltimes = 0
                total_time.setText("")
                total_salary.setText("")
                base_total_salary.setText("")
                holiday_total_salary.setText("")
                base_hourly_wage_text.setText("")
                holiday_hourly_wage_text.setText("")
                base_salary_button_click = false
                holiday_salary_button_click = false
                user_total_salary = 0
                user_base_salary = 0
                user_holiday_salary = 0
                user_base_hourly_wage_data = 0
                user_holiday_hourly_wage_data = 0
                user_base_work_count = 0
                user_holiday_work_count = 0
            }
            value == "C" -> {
                inputTextView.setText("")
                Resultbool = false
                totaltimes = 0
            }
            value == "⌫" -> {
                NumCheck = false
                if(Resultbool == false){
                    if(inputTextView.text.length == 0) inputTextView.setText("")
                    else inputTextView.setText(inputTextView.text.substring(0, inputTextView.text.length - 1))
                }
            }
            value == "+" || value == "-" || value == "=" -> {
                if(Resultbool == false){
                    currentOperation = value
                    val text = inputTextView.text.toString()
                    if (text.isNotEmpty() && text.last().isDigit()) {
                        if(NumCheck == false){
                            inputTextView.append(value)
                        }else{
                            val lastIndex = text.lastIndexOfAny(charArrayOf('+', '-'))
                            val newText = if (lastIndex != -1) {
                                val lastNumber = text.substring(lastIndex + 1)
                                val formattedLastNumber = formatTime(lastNumber)
                                text.substring(0, lastIndex + 1) + formattedLastNumber + value
                            } else {
                                formatTime(text) + value
                            }
                            inputTextView.setText(newText)
                        }
                    }
                    if(value == "=") {
                        Resultbool = true
                        calculateResult()
                    }
                }
            }
        }
    }

    private fun formatTime(input: String): String {
        return if (input.length == 2) {
            "0:$input"
        } else if (input.length == 1) {
            "0:0$input"
        } else {
            "${input.substring(0, input.length - 2)}:${input.substring(input.length - 2)}"
        }
    }

    private fun calculateResult() {
        val preinput = inputTextView.text.toString()
        if (preinput.isEmpty()) return
        val input = preinput.substring(0, preinput.length - 1)
        if (input.isEmpty()) return
        val parts = splitInput(input)

        var totalMinutes = 0

        parts.forEachIndexed { index, part ->
            var newpart = part
            if(part.startsWith("-")) {
                newpart = part.drop(1)
                val timeParts = newpart.split(":")
                if (timeParts.size == 2) {
                    val hours = timeParts[0].toInt()
                    val minutes = timeParts[1].toInt()
                    val timeInMinutes = hours * 60 + minutes
                    totalMinutes -= timeInMinutes
                }
            }else{
                if(part.startsWith("+")) {
                    newpart = part.drop(1)
                }
                val timeParts = newpart.split(":")
                if (timeParts.size == 2) {
                    val hours = timeParts[0].toInt()
                    val minutes = timeParts[1].toInt()
                    val timeInMinutes = hours * 60 + minutes
                    totalMinutes += timeInMinutes
                }
            }
            working_count++
        }

        totaltimes = totalMinutes
        val result = totalMinutes * weekdays_hourly_wage_data.toDouble() / 60.0
        val totalHours = totalMinutes / 60
        val totalMinutesRemainder = totalMinutes % 60
        val formattedTotalTime = String.format("%02d:%02d", totalHours, totalMinutesRemainder) // XX:XX形式にフォーマット

        total_time.setText(formattedTotalTime)
        inputTextView.append(formattedTotalTime)
        result_scrollView.fullScroll(ScrollView.FOCUS_DOWN)
    }

    private fun splitInput(input: String): List<String> {
        val parts = mutableListOf<String>()
        var currentPart = ""

        for (char in input) {
            if (char == '+' || char == '-') {
                if (currentPart.isNotEmpty()) {
                    parts.add(currentPart)
                }
                currentPart = char.toString()
            } else {
                currentPart += char
            }
        }

        if (currentPart.isNotEmpty()) {
            parts.add(currentPart)
        }

        return parts
    }
    //平日の時給設定ダイアログ
    fun showWeekdayHourlyWageInputDialog(context: Context, listener: (String) -> Unit) {
        val editText = EditText(context)
        editText.setText(weekdays_hourly_wage_data.toString())
        val dialog = AlertDialog.Builder(context)
            .setTitle("給料金額設定")
            .setMessage("平日の時給を入力してください:")
            .setView(editText)
            .setPositiveButton("次へ") { dialog, _ ->
                val text = editText.text.toString()
                if (text.toIntOrNull() != null) listener(text)
                else dialog.dismiss()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    //土休祝の時給設定ダイアログ
    fun showHolidayHourlyWageInputDialog(context: Context, listener: (String) -> Unit) {
        val editText = EditText(context)
        editText.setText(holidays_hourly_wage_data.toString())
        val dialog = AlertDialog.Builder(context)
            .setTitle("給料金額設定")
            .setMessage("土休祝の差額時給を入力してください:")
            .setView(editText)
            .setPositiveButton("次へ") { dialog, _ ->
                val text = editText.text.toString()
                if (text.toIntOrNull() != null) listener(text)
                else dialog.dismiss()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    //カスタム時給設定ダイアログ
    fun showCustomHourlyWageInputDialog(context: Context, listener: (String) -> Unit) {
        val editText = EditText(context)
        editText.setText(custom_hourly_wage_data.toString())
        val dialog = AlertDialog.Builder(context)
            .setTitle("給料金額設定")
            .setMessage("任意の時給を入力してください:")
            .setView(editText)
            .setPositiveButton("終了") { dialog, _ ->
                val text = editText.text.toString()
                if (text.toIntOrNull() != null) listener(text)
                else dialog.dismiss()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    //保存ダイアログ表示関数
    private fun showSaveDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save, null)

        val nameSpinner = dialogView.findViewById<Spinner>(R.id.nameSpinner)
        val yearEditText = dialogView.findViewById<EditText>(R.id.yearEditText)
        val monthEditText = dialogView.findViewById<EditText>(R.id.monthEditText)
        val baseworkcountEditText = dialogView.findViewById<EditText>(R.id.baseworkcountEditText)
        val holidayworkcountEditText = dialogView.findViewById<EditText>(R.id.holidayworkcountEditText)

        val save_database_date = getSharedPreferences("save_database_date", MODE_PRIVATE)
        val save_database_year = save_database_date.getInt("save_database_year", 2024) //年
        val save_database_month = save_database_date.getInt("save_database_month", 1) //月
        //EditTextに予めテキストを入力しておく
        yearEditText.setText(save_database_year.toString())
        monthEditText.setText(save_database_month.toString())
        baseworkcountEditText.setText(user_base_work_count.toString())
        holidayworkcountEditText.setText(user_holiday_work_count.toString())

        val employeeNames = getEmployeeNames()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, employeeNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        nameSpinner.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setTitle("保存")
            .setView(dialogView)
            .setPositiveButton("保存する") { _, _ ->
                val name = nameSpinner.selectedItem.toString()
                val year = yearEditText.text.toString().toInt()
                val month = monthEditText.text.toString().toInt()
                val base_work_count = baseworkcountEditText.text.toString().toInt()
                val holiday_work_count = holidayworkcountEditText.text.toString().toInt()
                saveDatabase(year, month, name, base_work_count, holiday_work_count)

                val save_database_date_editor: SharedPreferences.Editor = save_database_date.edit()
                save_database_date_editor.putInt("save_database_year", yearEditText.text.toString().toInt())
                save_database_date_editor.putInt("save_database_month", monthEditText.text.toString().toInt())
                save_database_date_editor.apply()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
    //従業員名取得関数
    private fun getEmployeeNames(): List<String> {
        val names = mutableListOf<String>()
        val db = employeeDBHelper.readableDatabase
        val cursor = db.query(
            EmployeeDatabaseContract.DatabaseEntry.TABLE_NAME,
            arrayOf(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_NAME),
            null, null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(EmployeeDatabaseContract.DatabaseEntry.EMPLOYEE_NAME))
                names.add(name)
            }
        }
        return names
    }
    //データベース保存関数
    private fun saveDatabase(year: Int, month: Int, name: String, base_work_count: Int, holiday_work_count: Int) {
        if(user_total_salary == 0){
            Toast.makeText(this, "保存するデータの情報が不足しています！", Toast.LENGTH_SHORT).show()
        }else{
            val db = salaryDBHelper.writableDatabase
            val values = ContentValues().apply {
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR, year)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH, month)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME, name)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY, user_total_salary)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY, user_base_salary)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS, user_base_work_hours)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES, user_base_work_times)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE, user_base_hourly_wage_data)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT, base_work_count)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY, user_holiday_salary)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS, user_holiday_work_hours)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES, user_holiday_work_times)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE, user_holiday_hourly_wage_data)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT, holiday_work_count)

            }

            val newRowId = db?.insert(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, null, values)
            if (newRowId != null && newRowId != -1L) {
                Toast.makeText(this, "データを保存しました！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "データを保存できませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //メニュー画面表示関数
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.option_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            handleMenuItemClick(menuItem)
        }
        popup.show()
    }
    //メニュー画面の処理リスナー
    private fun handleMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.see_salary_database -> {
                val intent = Intent(application, showDatabaseActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.employee_list -> {
                val intent = Intent(application, EmployeeListActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.setting_hourly_wage -> {
                showWeekdayHourlyWageInputDialog(this) { weekdays_hourly_wage_text ->
                    val hourly_wage_data = getSharedPreferences("salary_manager_hourly_wage_data", MODE_PRIVATE)
                    val weekdays_hourly_wage_editor: SharedPreferences.Editor = hourly_wage_data.edit()
                    weekdays_hourly_wage_editor.putInt("weekdays_hourly_wage", weekdays_hourly_wage_text.toInt())
                    weekdays_hourly_wage_editor.apply()
                    weekdays_hourly_wage_data = weekdays_hourly_wage_text.toInt()
                    showHolidayHourlyWageInputDialog(this) { holidays_hourly_wage_text ->
                        val holidays_hourly_wage_editor: SharedPreferences.Editor = hourly_wage_data.edit()
                        holidays_hourly_wage_editor.putInt("holiday_hourly_wage", holidays_hourly_wage_text.toInt())
                        holidays_hourly_wage_editor.apply()
                        holidays_hourly_wage_data = holidays_hourly_wage_text.toInt()
                        showCustomHourlyWageInputDialog(this) { custom_hourly_wage_text ->
                            val custom_hourly_wage_editor: SharedPreferences.Editor = hourly_wage_data.edit()
                            custom_hourly_wage_editor.putInt("custom_hourly_wage", custom_hourly_wage_text.toInt())
                            custom_hourly_wage_editor.apply()
                            custom_hourly_wage_data = custom_hourly_wage_text.toInt()
                        }
                    }
                }
                true
            }
            R.id.operation_manual -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("外部サイトへ移動")
                builder.setMessage("このリンクをクリックすると、外部サイトに移動します。よろしいですか？")

                builder.setPositiveButton("はい") { dialog, _ ->
                    dialog.dismiss()
                    openWebsite("https://github.com/oyunomin0109/SalaryManager/wiki/%E6%93%8D%E4%BD%9C%E3%83%9E%E3%83%8B%E3%83%A5%E3%82%A2%E3%83%AB")
                }

                builder.setNegativeButton("いいえ") { dialog, _ ->
                    dialog.dismiss()
                }

                val dialog: AlertDialog = builder.create()
                dialog.show()
                true
            }
            else -> false
        }
    }
    //外部のサイトへIntentする関数
    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
}