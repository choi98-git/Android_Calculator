package com.example.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.calculator.model.History
import org.w3c.dom.Text
import java.lang.NumberFormatException
import java.util.function.BinaryOperator

class MainActivity : AppCompatActivity() {


    private val expressionTextView: TextView by lazy {
        findViewById<TextView>(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy {
        findViewById<TextView>(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById<View>(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy {
        findViewById<LinearLayout>(R.id.historyLinearLayout)
    }

    lateinit var db: AppDatabase

    private var isOperator = false

    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    fun buttonClicked(v: View) {
        when (v.id) {
            R.id.Button0 -> numberButtonClicked("0")
            R.id.Button1 -> numberButtonClicked("1")
            R.id.Button2 -> numberButtonClicked("2")
            R.id.Button3 -> numberButtonClicked("3")
            R.id.Button4 -> numberButtonClicked("4")
            R.id.Button5 -> numberButtonClicked("5")
            R.id.Button6 -> numberButtonClicked("6")
            R.id.Button7 -> numberButtonClicked("7")
            R.id.Button8 -> numberButtonClicked("8")
            R.id.Button9 -> numberButtonClicked("9")
            R.id.ButtonPlus -> operatorButtonClicked("+")
            R.id.ButtonMinus -> operatorButtonClicked("-")
            R.id.ButtonMulti -> operatorButtonClicked("*")
            R.id.ButtonModulo -> operatorButtonClicked("%")
            R.id.ButtonDivider -> operatorButtonClicked("/")

        }
    }

    private fun numberButtonClicked(number: String) {

        // 연산자를 입력하고 온 경우
        if (isOperator){
            expressionTextView.append(" ")
        }
        isOperator = false

        val expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15){
            Toast.makeText(this, "15자리 까지만 사용할 수 있습니다",Toast.LENGTH_SHORT).show()
            return
        }else if ( number == "0" && expressionText.last().isEmpty()){
            Toast.makeText(this, "수의 맨 앞에 0이 올 수 없습니다",Toast.LENGTH_SHORT).show()
            return
        }
        expressionTextView.append(number)
        resultTextView.text = calculateExpression()

    }

    private fun operatorButtonClicked(operator: String) {

        // 제일 먼저 연산자가 올 경우 무시
        if (expressionTextView.text.isEmpty()){
            return
        }

        when{
            isOperator ->{
                val text = expressionTextView.text.toString()
                // 연산자를 입력 후 다른 연산자를 눌렀을 때 첫 번째로 입력했던 연산자를 지우고 나중에 입력한 연산자를 추가
                expressionTextView.text = text.dropLast(1) + operator
            }
            // 이미 연산자를 사용한 경우
            hasOperator ->{
                Toast.makeText(this, "연산자는 한 번만 사용할 수 있습니다!",Toast.LENGTH_SHORT).show()
                return
            }

            else -> {
                expressionTextView.append(" $operator")
            }
        }

        // 연산자의 텍스트를 색을 green으로 변경
        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length - 1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true

    }

    private fun calculateExpression(): String{

        val expressionTexts = expressionTextView.text.toString().split(" ")

        if(hasOperator.not() || expressionTexts.size != 3 ){
            return ""
        }else if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            return ""
        }
        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        return when(op){
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
                else -> ""
        }
    }

    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }


    fun resultButtonClicked(v: View) {
        val expressionTexts = expressionTextView.text.toString().split(" ")

        // 비어있가나 첫 번째 수만 입력된 경우
        if (expressionTextView.text.isEmpty() || expressionTexts.size == 1) {
            return
        }

        // 첫 번째 수와 연산자까지만 입력된 경우
        if (expressionTexts.size != 3 && hasOperator){
            Toast.makeText(this, "아직 완성되지 않은 수식입니다!!",Toast.LENGTH_SHORT).show()
            return
        }
        // 숫자로 치환이 안된 경우
        if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()) {
            Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        //DB에 계산 기록 저장
        Thread(Runnable {
            db.historyDao().insertHistory(History(null, expressionText, resultText))
        }).start()

        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible = true

        historyLinearLayout.removeAllViews()
        //DB에 저장된 데이터를 가져와서 새로운 화면에 보여줌
        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach {
                runOnUiThread{
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()
    }

    fun closeHistoryButtonClicked(v: View){
        historyLayout.isVisible = false
    }

    // 기록 삭제
    fun historyClearButtonClicked(v: View){
        historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()

    }
}

// 확장함수 선언
fun String.isNumber(): Boolean{
    return try {
        this.toBigInteger()
        return true
    }catch (e: NumberFormatException){
        return false
    }
}