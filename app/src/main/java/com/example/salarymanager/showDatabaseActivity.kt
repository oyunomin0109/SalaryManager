package com.example.salarymanager

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException

// 主なアクティビティクラス
class showDatabaseActivity : AppCompatActivity() {
    // プロパティの定義
    private lateinit var dbHelper: SalaryDatabaseHelper // データベースヘルパー
    private lateinit var infoTextView: TextView // 情報表示用のテキストビュー
    private lateinit var dataTable: TableLayout // データ表示用のテーブル
    private lateinit var yearSpinner: Spinner // 年選択用スピナー
    private lateinit var monthSpinner: Spinner // 月選択用スピナー
    private lateinit var viewButton: Button // データ表示ボタン
    private lateinit var excelButton: ImageButton // Excel保存ボタン
    private lateinit var homeButton: ImageButton // ホームボタン
    private lateinit var settingExcelButton: ImageButton //excel設定ボタン
    private lateinit var DetaileddisplaySwitch: Switch // 詳細表示スイッチ

    companion object {
        private const val CREATE_FILE_REQUEST_CODE = 123 // Excelファイル作成リクエストコード
    }

    // アクティビティ作成時の処理
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database) // レイアウトをセット

        initViews() // ビューを初期化
        setupDatabase() // データベースを設定

        // 年スピナーの選択リスナー設定
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedYear = parent?.getItemAtPosition(position).toString()
                // 選択した年が「All」でない場合、月スピナーを設定
                if (selectedYear != "All") {
                    setupMonthSpinner(selectedYear.toInt())
                    monthSpinner.visibility = View.VISIBLE // スピナーを表示
                } else {
                    monthSpinner.visibility = View.GONE // スピナーを非表示
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {} // 何も選択されていない場合の処理
        }

        // データ表示ボタンのクリックリスナー
        viewButton.setOnClickListener {
            val selectedYear = yearSpinner.selectedItem.toString() // 選択した年を取得
            // 月スピナーが表示されている場合、選択した月を取得
            val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null
            dataTable.visibility = View.VISIBLE // データテーブルを表示
            infoTextView.visibility = View.INVISIBLE // 情報テキストを非表示
            showData(selectedYear, selectedMonth) // データを表示
        }

        // Excel保存ボタンのクリックリスナー
        excelButton.setOnClickListener { saveAsExcel() }
        // ホームボタンのクリックリスナー
        homeButton.setOnClickListener { finish() }

        //保存ダイアログを表示
        settingExcelButton.setOnClickListener {
            showExcelSettingDialog()
        }

        // 詳細表示スイッチの変更リスナー
        DetaileddisplaySwitch.setOnCheckedChangeListener { _, _ ->
            val selectedYear = yearSpinner.selectedItem.toString()
            val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null
            showData(selectedYear, selectedMonth) // データを再描画
        }
    }

    // ビューの初期化
    private fun initViews() {
        infoTextView = findViewById(R.id.textView) // テキストビューを取得
        dataTable = findViewById(R.id.tableLayout) // テーブルレイアウトを取得
        yearSpinner = findViewById(R.id.year_spinner) // 年スピナーを取得
        monthSpinner = findViewById(R.id.month_spinner) // 月スピナーを取得
        viewButton = findViewById(R.id.view_button) // 表示ボタンを取得
        excelButton = findViewById(R.id.excelButton) // Excelボタンを取得
        settingExcelButton = findViewById(R.id.settingExcelButton)
        homeButton = findViewById(R.id.homeButton) // ホームボタンを取得
        DetaileddisplaySwitch = findViewById(R.id.Detailed_display_Switch) // 詳細表示スイッチを取得
    }

    // データベースの初期設定
    private fun setupDatabase() {
        dbHelper = SalaryDatabaseHelper(this) // データベースヘルパーのインスタンスを生成
        val db = dbHelper.readableDatabase // 読み取り可能なデータベースを取得
        if (db == null) {
            infoTextView.text = "データベースが存在しません" // エラーメッセージを表示
            setViewVisibility(View.INVISIBLE, dataTable, yearSpinner, monthSpinner, viewButton) // ビューを非表示
        } else {
            infoTextView.text = "閲覧したいデータを選択してください" // 情報メッセージを表示
            setViewVisibility(View.INVISIBLE, dataTable, monthSpinner) // 月スピナーを非表示
            setupYearSpinner() // 年スピナーを設定
        }
    }

    // 年スピナーの設定
    private fun setupYearSpinner() {
        val db = dbHelper.readableDatabase // 読み取り可能なデータベースを取得
        val years = mutableListOf("All") // 年リストの初期化

        // 年のデータを取得
        db.rawQuery("SELECT DISTINCT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME}", null).use { cursor ->
            while (cursor.moveToNext()) {
                // 一意な年をリストに追加
                years.add(cursor.getInt(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR)).toString())
            }
        }

        // アダプターを作成し、スピナーに設定
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = adapter
    }

    // 月スピナーの設定
    private fun setupMonthSpinner(selectedYear: Int) {
        val db = dbHelper.readableDatabase // 読み取り可能なデータベースを取得
        val months = mutableListOf("All") // 月リストの初期化

        // 選択した年に基づく月のデータを取得
        db.rawQuery("SELECT DISTINCT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME} WHERE ${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?", arrayOf(selectedYear.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                // 一意な月をリストに追加
                months.add(cursor.getInt(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH)).toString())
            }
        }

        // アダプターを作成し、スピナーに設定
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter
    }

    // データ表示処理
    private fun showData(selectedYear: String, selectedMonth: String?) {
        dataTable.removeAllViews() // テーブルの内容をクリア
        val db = dbHelper.readableDatabase // 読み取り可能なデータベースを取得

        // クエリ選択条件の設定
        val (selection, selectionArgs) = if (selectedYear == "All") {
            Pair(null, null) // 年が「All」の場合、全てのデータを取得
        } else {
            if (selectedMonth != null && selectedMonth != "All") {
                Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ? AND ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} = ?", arrayOf(selectedYear, selectedMonth)) // 年と月に基づいたクエリ条件
            } else {
                Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?", arrayOf(selectedYear)) // 年に基づいたクエリ条件
            }
        }

        // クエリでデータを取得
        db.query(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null).use { cursor ->
            if (cursor == null || cursor.count == 0) {
                infoTextView.text = "データが存在しません" // データが存在しないメッセージを表示
                dataTable.visibility = View.INVISIBLE // テーブルを非表示
                return
            }
            addTableHeader() // ヘッダーを追加

            // データ行の追加
            while (cursor.moveToNext()) {
                // データの取得
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                val year = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR))
                val month = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME))
                val totalSalary = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))
                val baseHours = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS))
                val baseTimes = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES))
                val holidayHours = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS))
                val holidayTimes = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES))

                // スイッチがONかOFFかで表示内容を切り替える
                if (DetaileddisplaySwitch.isChecked) { // ONの場合はすべての情報を表示
                    val baseSalary = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY))
                    val baseTimes = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES))
                    val baseHourlyWage = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE))
                    val baseWorkingCount = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT))
                    val holidaySalary = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY))
                    val holidayHourlyWage = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE))
                    val holidayWorkingCount = cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT))

                    // 行をテーブルに追加
                    addTableRow(id, year, month, name, totalSalary, baseSalary, baseHours, baseTimes, baseHourlyWage, baseWorkingCount, holidaySalary, holidayHours, holidayTimes, holidayHourlyWage, holidayWorkingCount)
                } else { // OFFの場合は選択した情報のみ表示
                    addTableRow(id, year, month, name, "$baseHours:$baseTimes", "$holidayHours:$holidayTimes")
                }
            }

            excelButton.visibility = View.VISIBLE // Excelボタンを表示
        }
    }

    // テーブルヘッダーを追加
    private fun addTableHeader() {
        val headerRow = TableRow(this) // ヘッダー行を作成

        // スイッチがONの場合、すべてのカラムヘッダを追加
        if (DetaileddisplaySwitch.isChecked) {
            val headers = listOf("年", "月", "名前", "総給料", "平日給", "平日時", "平日分", "平日時給", "平日勤務回", "土休差額", "土休時", "土休分", "土休時給差額", "土休勤務回")
            headers.forEach { header ->
                val headerView = TextView(this).apply {
                    text = header
                    gravity = Gravity.LEFT
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(Color.rgb(224, 224, 224)) // ヘッダーの背景色を設定
                }
                headerRow.addView(headerView) // ヘッダーを行に追加
            }
        } else {
            // スイッチがOFFの場合、必要なカラムヘッダだけを追加
            val headers = listOf("年", "月", "名前", "平日勤務", "休日勤務")
            headers.forEach { header ->
                val headerView = TextView(this).apply {
                    text = header
                    gravity = Gravity.LEFT
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(Color.rgb(224, 224, 224)) // ヘッダーの背景色を設定
                }
                headerRow.addView(headerView) // ヘッダーを行に追加
            }
        }
        dataTable.addView(headerRow) // テーブルにヘッダー行を追加
        addSeparator() // セパレーターを追加
    }

    // テーブル行を追加
    private fun addTableRow(id: Long, vararg values: String) {
        val tableRow = TableRow(this) // 新しいテーブル行を作成
        tableRow.tag = id // 行にIDを設定

        // 各値をテーブルの行に追加
        values.forEach { value ->
            val textView = TextView(this).apply {
                text = value // テキストを設定
                gravity = Gravity.LEFT
                setPadding(16, 16, 16, 16) // パディングを設定
            }
            tableRow.addView(textView) // テキストビューを行に追加
        }

        // 行がクリックされたときのリスナーを設定
        tableRow.setOnClickListener { showDataDialog(id, *values) } // ダイアログを表示
        dataTable.addView(tableRow) // テーブルに行を追加
        addSeparator() // セパレーターを追加
    }

    // セパレーターを追加
    private fun addSeparator() {
        val separator = View(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1) // セパレーターのサイズを設定
            setBackgroundColor(Color.BLACK) // セパレーターの色を設定
        }
        dataTable.addView(separator) // テーブルにセパレーターを追加
    }

    // データダイアログを表示
    private fun showDataDialog(id: Long, vararg values: String) {
        // 詳細情報を構築
        val details = if (DetaileddisplaySwitch.isChecked) {
            "ID: $id\n" +
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
                    "土休祝勤務回数: ${values[13]}\n"
        } else {
            // OFFの場合は基本情報のみ表示
            "ID: $id\n" +
                    "従業員名: ${values[2]}\n" +
                    "年: ${values[0]}\n" +
                    "月: ${values[1]}\n" +
                    "平日勤務時間: ${values[3]}\n" +
                    "休日勤務時間: ${values[4]}\n"
        }

        // アラートダイアログを作成
        val builder = AlertDialog.Builder(this)
            .setTitle("詳細データ") // ダイアログのタイトル
            .setMessage(details) // ダイアログに表示するメッセージ

        // 詳細表示スイッチがONの場合のみ修正ボタンを表示
        if (DetaileddisplaySwitch.isChecked) {
            builder.setPositiveButton("修正") { dialog, _ ->
                showEditDialog(id, values) // 修正ダイアログを表示
            }
        } else {
            // スイッチがOFFの場合は修正ボタンを無効にする
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        }
        // 削除ボタンを設定
        builder.setNegativeButton("削除") { dialog, _ -> confirmDeleteData(id) // 削除確認ダイアログを表示
            dialog.dismiss() }

        builder.create().show() // ダイアログを表示
    }

    // 修正ダイアログの表示
    private fun showEditDialog(id: Long, values: Array<out String>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_data, null) // 編集用ダイアログのレイアウトをinflate

        // 現在のデータを設定
        val baseHoursEditText: EditText = dialogView.findViewById(R.id.baseHoursEditText) // 平日勤務時間（時）の編集ボックス
        val baseMinutesEditText: EditText = dialogView.findViewById(R.id.baseMinutesEditText) // 平日勤務時間（分）の編集ボックス
        val holidayHoursEditText: EditText = dialogView.findViewById(R.id.holidayHoursEditText) // 休日勤務時間（時）の編集ボックス
        val holidayMinutesEditText: EditText = dialogView.findViewById(R.id.holidayMinutesEditText) // 休日勤務時間（分）の編集ボックス
        val workingCountEditText: EditText = dialogView.findViewById(R.id.workingCountEditText) // 勤務回数の編集ボックス
        val holidayCountEditText: EditText = dialogView.findViewById(R.id.holidayCountEditText) // 休日勤務回数の編集ボックス

        // 現在のデータで入力フィールドを初期化
        baseHoursEditText.setText(values[5]) // 平日勤務時間（時）を設定
        baseMinutesEditText.setText(values[6]) // 平日勤務時間（分）を設定
        holidayHoursEditText.setText(values[10]) // 休日勤務時間（時）を設定
        holidayMinutesEditText.setText(values[11]) // 休日勤務時間（分）を設定
        workingCountEditText.setText(values[8]) // 勤務回数を設定
        holidayCountEditText.setText(values[13]) // 休日勤務回数を設定

        // ダイアログを生成
        val dialog = AlertDialog.Builder(this)
            .setTitle("データ修正") // ダイアログのタイトル
            .setView(dialogView) // ダイアログにビューを設定
            .create()

        // ボタンの設定
        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener { dialog.dismiss() } // キャンセルボタン
        dialogView.findViewById<Button>(R.id.updateButton).setOnClickListener {
            // 新しい値を取得
            val newBaseHours = baseHoursEditText.text.toString().toIntOrNull() ?: 0 // 現在の時の値を取得
            val newBaseMinutes = baseMinutesEditText.text.toString().toIntOrNull() ?: 0 // 現在の分の値を取得
            val newHolidayHours = holidayHoursEditText.text.toString().toIntOrNull() ?: 0 // 休日勤務時間（時）の値を取得
            val newHolidayMinutes = holidayMinutesEditText.text.toString().toIntOrNull() ?: 0 // 休日勤務時間（分）の値を取得
            val newWorkingCount = workingCountEditText.text.toString().toIntOrNull() ?: 0 // 勤務回数の値を取得
            val newHolidayCount = holidayCountEditText.text.toString().toIntOrNull() ?: 0 // 休日勤務回数の値を取得

            // 平日時給と土休祝時給をデータベースから取得
            val db = dbHelper.readableDatabase
            val baseHourlyWage = db.rawQuery("SELECT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME} WHERE ${BaseColumns._ID} = ?", arrayOf(id.toString())).use { cursor ->
                // カーソルが結果をもっていたら時給を取得
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE)).toInt()
                } else {
                    0 // 無い場合は0を返す
                }
            }
            val holidayHourlyWage = db.rawQuery("SELECT ${SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE} FROM ${SalaryDatabaseContract.DatabaseEntry.TABLE_NAME} WHERE ${BaseColumns._ID} = ?", arrayOf(id.toString())).use { cursor ->
                // カーソルが結果をもっていたら時給を取得
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE)).toInt()
                } else {
                    0 // 無い場合は0を返す
                }
            }

            // 計算を行う
            val totalBaseSalary = (newBaseHours * 60 + newBaseMinutes) * baseHourlyWage / 60 // 基本給計算
            val totalHolidaySalary = (newHolidayHours * 60 + newHolidayMinutes) * holidayHourlyWage / 60 // 休日給計算
            val totalSalary = totalBaseSalary + totalHolidaySalary // 総給計算

            // データベースを更新する
            val valuesToUpdate = ContentValues().apply {
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS, newBaseHours)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES, newBaseMinutes)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS, newHolidayHours)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES, newHolidayMinutes)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT, newWorkingCount)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT, newHolidayCount)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY, totalBaseSalary)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY, totalHolidaySalary)
                put(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY, totalSalary) // 更新する全データを準備
            }

            // データの更新を行う
            val updatedRows = db.update(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, valuesToUpdate, "${BaseColumns._ID} = ?", arrayOf(id.toString()))
            if (updatedRows > 0) {
                Toast.makeText(this, "データを修正しました", Toast.LENGTH_SHORT).show() // 修正成功メッセージ
                dialog.dismiss() // ダイアログを閉じる
                showData(yearSpinner.selectedItem.toString(), if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null) // データを再表示
            } else {
                Toast.makeText(this, "データの修正に失敗しました", Toast.LENGTH_SHORT).show() // 修正失敗メッセージ
            }
        }
        dialog.show() // ダイアログを表示
    }

    // データ削除の確認ダイアログを表示
    private fun confirmDeleteData(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("確認") // タイトル
            .setMessage("本当に削除しますか？") // メッセージ
            .setPositiveButton("はい") { dialog, _ ->
                deleteData(id) // データを削除
                dialog.dismiss() // ダイアログを閉じる
            }
            .setNegativeButton("いいえ") { dialog, _ -> dialog.dismiss() } // 処理をキャンセル
            .create()
            .show() // ダイアログを表示
    }

    // データ削除処理
    private fun deleteData(id: Long) {
        val db = dbHelper.writableDatabase // 書き込み可能なデータベースを取得
        val selection = "${BaseColumns._ID} = ?" // 削除条件
        val selectionArgs = arrayOf(id.toString()) // IDを指定

        // データを削除
        val deletedRows = db.delete(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, selection, selectionArgs)
        if (deletedRows > 0) {
            Toast.makeText(this, "データを削除しました", Toast.LENGTH_SHORT).show() // 削除成功メッセージ
            showData(yearSpinner.selectedItem.toString(), if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null) // データを再表示
        } else {
            Toast.makeText(this, "データの削除に失敗しました", Toast.LENGTH_SHORT).show() // 削除失敗メッセージ
        }
    }

    // Excelファイル保存処理
    private fun saveAsExcel() {
        val selectedYear = yearSpinner.selectedItem.toString() // 選択した年を取得
        val selectedMonth = if (monthSpinner.visibility == View.VISIBLE) { monthSpinner.selectedItem.toString().toIntOrNull() } else { null } // 選択した月を取得
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { // ドキュメント作成のインテント
            addCategory(Intent.CATEGORY_OPENABLE) // オープン可能なカテゴリを追加
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // Excelファイルタイプを設定
            putExtra(Intent.EXTRA_TITLE, "data.xlsx") // ファイル名を設定
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE, Bundle().apply { // インテントを開始
            putString("year", selectedYear) // 年を追加
            putInt("month", selectedMonth ?: -1) // 月を追加
        })
    }

    // インテントの結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.let { uri -> // URIが取得できたら
                saveExcelToUri(uri, yearSpinner.selectedItem.toString(), if (monthSpinner.visibility == View.VISIBLE) monthSpinner.selectedItem.toString() else null) // Excelを保存
            }
        }
    }

    // Excelにデータを保存
    private fun saveExcelToUri(uri: Uri, selectedYear: String, selectedMonth: String?) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream -> // URIに対するストリームを取得
                val workbook: Workbook = XSSFWorkbook() // 新しいExcelワークブックを作成
                val sheet: Sheet = workbook.createSheet("データ") // シート作成
                val db = dbHelper.readableDatabase // 読み取り可能なデータベースを取得

                val save_excel_setting_date = getSharedPreferences("save_excel_setting_date", MODE_PRIVATE)
                val save_header_switch = save_excel_setting_date.getBoolean("save_header_switch", false)
                val save_totalsalary_switch = save_excel_setting_date.getBoolean("save_totalsalary_switch", true)
                val save_column_text = save_excel_setting_date.getInt("save_column_text", 7)


                // データ行のタイトル
                val headers = arrayOf(
                    "従業員名", "年/月", "基本給料", "基本給料", "勤務回数", "時間", "分", "基本時給", "土休差額", "土休差額", "勤務回数", "時間", "分", "土休日差額時給", "合計給料額"
                )
                var rowIndex = 0 // 行インデックスの初期化

                // タイトル行の追加
                if (save_header_switch) {
                    headers.forEachIndexed { index, header ->
                        val row = sheet.createRow(rowIndex) // 新しい行を作成
                        row.createCell(0).setCellValue(header) // セルにタイトルを設定
                        rowIndex++ // 行インデックスを増加
                    }
                }

                // データ行を設定
                val (selection, selectionArgs) = if (selectedYear == "All") {
                    Pair(null, null) // 年が「All」の場合、全てのデータを取得
                } else {
                    if (selectedMonth != null && selectedMonth != "All") {
                        Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ? AND ${SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH} = ?", arrayOf(selectedYear, selectedMonth)) // 年と月に基づいたクエリ条件
                    } else {
                        Pair("${SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR} = ?", arrayOf(selectedYear)) // 年に基づいたクエリ条件
                    }
                }

                //合計給料
                var total_salary = 0
                var upcount = 0
                // データ引き出し
                db.query(SalaryDatabaseContract.DatabaseEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) { // データが存在する場合
                        var columnIndex = 0 // データ格納開始列
                        if (save_header_switch) columnIndex = 1
                        var addedcount = 0
                        do {
                            rowIndex = 15 * upcount
                            // 各列の値を取得
                            total_salary += cursor.getInt(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))
                            val rowValues = arrayOf(
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_NAME)),
                                "${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_YEAR))}/${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_MONTH))}",
                                "基本給料",
                                "¥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_SALARY))}",
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_WORKING_COUNT)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURS)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_TIMES)),
                                "¥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_BASE_HOURLY_WAGE))}",
                                "土休差額",
                                "¥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_SALARY))}",
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_WORKING_COUNT)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURS)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_TIMES)),
                                "¥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_HOLIDAY_HOURLY_WAGE))}",
                                "¥${cursor.getString(cursor.getColumnIndexOrThrow(SalaryDatabaseContract.DatabaseEntry.COLUMN_TOTAL_SALARY))}"
                            )

                            rowValues.forEach { value ->
                                if (columnIndex == save_column_text) {
                                    upcount++
                                    rowIndex = 15 * upcount
                                    columnIndex = 0 // 列インデックスを増加

                                }
                                val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex) // 行を取得または新しく作成
                                val cell = row.createCell(columnIndex) // セルを作成
                                cell.setCellValue(value) // セルにデータを設定

                                // 罫線のスタイルを追加
                                val cellStyle = workbook.createCellStyle()
                                cellStyle.borderTop = BorderStyle.THIN // 上罫線
                                cellStyle.borderBottom = BorderStyle.THIN // 下罫線
                                cellStyle.borderLeft = BorderStyle.THIN // 左罫線
                                cellStyle.borderRight = BorderStyle.THIN // 右罫線
                                cell.cellStyle = cellStyle // セルにスタイルを設定
                                // 6行に達したら、次の書き込みに備えて行インデックスをリセット
                                rowIndex++ // 行インデックスを増加
                                Log.d("列行", "行：" + rowIndex.toString() + "　列：" + columnIndex.toString()) // ログ出力
                            }
                            columnIndex++ // 列インデックスを増加
                            addedcount++
                        } while (cursor.moveToNext()) // カーソルを次へ
                    }
                    if (save_totalsalary_switch) {
                        val row2 = sheet.createRow(15 * (upcount + 1)) // 行を取得または新しく作成
                        val cell2 = row2.createCell(1) // セルを作成
                        cell2.setCellValue("¥" + total_salary.toString()) // セルにデータを設定
                    }
                }

                // 列幅を設定
                for (i in 0 until sheet.getRow(0).lastCellNum) {
                    sheet.setColumnWidth(i, 4000) // 列幅を4000に設定
                }

                workbook.write(outputStream) // Excelに書き込み
                workbook.close() // ワークブックを閉じる
                Toast.makeText(this, "Excelとして保存しました", Toast.LENGTH_SHORT).show() // 保存成功メッセージ
            }
        } catch (e: IOException) {
            Log.e("Excel", "Error writing Excel", e) // エラーロギング
            Toast.makeText(this, "保存ができませんでした", Toast.LENGTH_SHORT).show() // 保存失敗メッセージ
        }
    }

    // 指定したビューの可視性を設定
    private fun setViewVisibility(visibility: Int, vararg views: View) {
        views.forEach { it.visibility = visibility } // 各ビューの可視性を設定
    }

    private fun showExcelSettingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_excel_settings, null)
        val headerSwitch = dialogView.findViewById<Switch>(R.id.switch_include_headers)
        val totalsalarySwitch = dialogView.findViewById<Switch>(R.id.switch_include_total_salary)
        val columnEditText = dialogView.findViewById<EditText>(R.id.edittext_column_count)

        val save_excel_setting_date = getSharedPreferences("save_excel_setting_date", MODE_PRIVATE)
        val save_header_switch = save_excel_setting_date.getBoolean("save_header_switch", false)
        val save_totalsalary_switch = save_excel_setting_date.getBoolean("save_totalsalary_switch", true)
        val save_column_text = save_excel_setting_date.getInt("save_column_text", 7)

        // 整数を文字列に変換して設定
        columnEditText.setText(save_column_text.toString())
        headerSwitch.isChecked = save_header_switch
        totalsalarySwitch.isChecked = save_totalsalary_switch

        AlertDialog.Builder(this)
            .setTitle("設定")
            .setView(dialogView)
            .setPositiveButton("変更する") { _, _ ->
                val count = columnEditText.text.toString().toInt()
                val save_excel_setting_date_editor: SharedPreferences.Editor = save_excel_setting_date.edit()
                save_excel_setting_date_editor.putBoolean("save_header_switch", headerSwitch.isChecked)
                save_excel_setting_date_editor.putBoolean("save_totalsalary_switch", totalsalarySwitch.isChecked)
                save_excel_setting_date_editor.putInt("save_column_text", count)
                save_excel_setting_date_editor.apply()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}