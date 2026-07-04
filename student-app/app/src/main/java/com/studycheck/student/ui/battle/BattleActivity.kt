package com.studycheck.student.ui.battle

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.studycheck.student.R
import com.studycheck.student.data.*
import com.studycheck.student.databinding.ActivityBattleBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.util.TouchFeedback
import kotlinx.coroutines.*

class BattleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBattleBinding
    private var battleState: BattleState? = null
    private var formulas = listOf<FormulaInfo>()
    private val handler = Handler(Looper.getMainLooper())
    private var isPlayerTurn = true
    private var battleOver = false

    private val skinEmojis = mapOf(
        "default" to "🐱", "dog" to "🐶", "bunny" to "🐰", "fox" to "🦊",
        "panda" to "🐼", "unicorn" to "🦄", "dragon" to "🐲", "alien" to "👽"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBattleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TouchFeedback.applyAll(
            binding.btnStartBattle, binding.btnFormula1,
            binding.btnFormula2, binding.btnFormula3, binding.btnFormula4
        )

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnStartBattle.setOnClickListener { startBattle() }
        binding.btnFormula1.setOnClickListener { attackWith("f1") }
        binding.btnFormula2.setOnClickListener { attackWith("f2") }
        binding.btnFormula3.setOnClickListener { attackWith("f3") }
        binding.btnFormula4.setOnClickListener { attackWith("f4") }

        loadFormulas()
    }

    private fun loadFormulas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getBattleFormulas()
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        formulas = response.data
                        updateFormulaButtons()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateFormulaButtons() {
        val sorted = formulas.sortedByDescending { it.mastery }.take(4)
        val buttons = listOf(binding.btnFormula1, binding.btnFormula2, binding.btnFormula3, binding.btnFormula4)
        for (i in sorted.indices) {
            val f = sorted[i]
            buttons[i].text = "${f.name}\n${if (f.mastery >= 100) "⚡自动" else "📝解题"} · ${f.damage}伤"
            buttons[i].tag = f.id
            buttons[i].visibility = View.VISIBLE
        }
        for (i in sorted.size until 4) {
            buttons[i].visibility = View.GONE
        }
    }

    private fun startBattle() {
        binding.layoutStart.visibility = View.GONE
        binding.layoutBattle.visibility = View.VISIBLE
        binding.layoutLog.visibility = View.VISIBLE
        binding.tvLog.text = ""
        battleOver = false
        isPlayerTurn = true
        updateFormulaButtons()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.startBattle()
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        battleState = response.data
                        updateBattleUI()
                        addLog("⚔️ 对战开始！")
                        addLog("${battleState?.player?.pet_name} VS ${battleState?.opponent?.name}")
                        setFormulaButtonsEnabled(true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BattleActivity, "开始失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBattleUI() {
        val state = battleState ?: return
        binding.tvPlayerEmoji.text = skinEmojis[state.player.pet_skin] ?: "🐱"
        binding.tvPlayerName.text = state.player.pet_name
        binding.tvPlayerLevel.text = "Lv.${state.player.level}"
        binding.pbPlayerHp.max = state.player.max_hp
        binding.pbPlayerHp.progress = state.player.hp
        binding.tvPlayerHp.text = "${state.player.hp}/${state.player.max_hp}"

        binding.tvOpponentEmoji.text = state.opponent.emoji
        binding.tvOpponentName.text = state.opponent.name
        binding.tvOpponentLevel.text = "Lv.${state.opponent.level}"
        binding.pbOpponentHp.max = state.opponent.max_hp
        binding.pbOpponentHp.progress = state.opponent.hp
        binding.tvOpponentHp.text = "${state.opponent.hp}/${state.opponent.max_hp}"
    }

    private fun attackWith(formulaId: String) {
        if (!isPlayerTurn || battleOver) return
        isPlayerTurn = false
        setFormulaButtonsEnabled(false)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.battleAttack(
                    mapOf("formula_id" to formulaId, "skip_solve" to true)
                )
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        if (response.data.auto) {
                            // 熟练度100%，自动释放
                            applyDamage(response.data.damage ?: 0, false)
                            addLog(response.data.msg)
                            handler.postDelayed({ opponentTurn() }, 1200)
                        } else {
                            // 需要解题
                            showSolveDialog(response.data)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BattleActivity, "网络错误", Toast.LENGTH_SHORT).show()
                    isPlayerTurn = true
                    setFormulaButtonsEnabled(true)
                }
            }
        }
    }

    private fun showSolveDialog(attackResult: AttackResult) {
        val problem = attackResult.problem ?: return
        val formula = attackResult.formula ?: return

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val tvQuestion = TextView(this).apply {
            text = problem.question
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, 12)
        }
        val tvHint = TextView(this).apply {
            text = "提示：${problem.hint}"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
            setPadding(0, 0, 0, 16)
        }
        val answerInput = EditText(this).apply {
            hint = "输入答案"
            setSingleLine()
        }

        layout.addView(tvQuestion)
        layout.addView(tvHint)
        layout.addView(answerInput)

        AlertDialog.Builder(this)
            .setTitle("释放 ${formula.name}")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("提交") { _, _ ->
                val answer = answerInput.text.toString().trim()
                submitSolve(formula.id, answer, problem)
            }
            .show()
    }

    private fun submitSolve(formulaId: String, answer: String, problem: ProblemInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.battleSolve(
                    mapOf(
                        "formula_id" to formulaId,
                        "answer" to answer,
                        "problem" to mapOf(
                            "question" to problem.question,
                            "answer" to problem.answer,
                            "hint" to problem.hint,
                            "formula_name" to problem.formula_name,
                            "formula" to problem.formula
                        )
                    )
                )
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        val result = response.data
                        addLog(result.msg)
                        if (result.correct) {
                            applyDamage(result.damage, false)
                            if (result.pet_exp > 0) {
                                addLog("🐾 宠物获得 ${result.pet_exp} 经验")
                            }
                        }
                        updateFormulaButtons()
                        handler.postDelayed({ opponentTurn() }, 1200)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BattleActivity, "网络错误", Toast.LENGTH_SHORT).show()
                    isPlayerTurn = true
                    setFormulaButtonsEnabled(true)
                }
            }
        }
    }

    private fun applyDamage(damage: Int, toPlayer: Boolean) {
        val state = battleState ?: return
        if (toPlayer) {
            val newHp = maxOf(0, state.player.hp - damage)
            battleState = state.copy(
                player = state.player.copy(hp = newHp)
            )
            animateHpBar(binding.pbPlayerHp, binding.tvPlayerHp, newHp, state.player.max_hp)
            if (newHp <= 0) {
                endBattle(false)
            }
        } else {
            val newHp = maxOf(0, state.opponent.hp - damage)
            battleState = state.copy(
                opponent = state.opponent.copy(hp = newHp)
            )
            animateHpBar(binding.pbOpponentHp, binding.tvOpponentHp, newHp, state.opponent.max_hp)
            if (newHp <= 0) {
                endBattle(true)
            }
        }
    }

    private fun animateHpBar(bar: android.widget.ProgressBar, tv: TextView, newHp: Int, maxHp: Int) {
        android.animation.ValueAnimator.ofInt(bar.progress, newHp).apply {
            duration = 300
            addUpdateListener { anim ->
                val value = anim.animatedValue as Int
                bar.progress = value
                tv.text = "$value/$maxHp"
            }
            start()
        }
    }

    private fun opponentTurn() {
        if (battleOver) return
        val state = battleState ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.opponentAttack(
                    mapOf(
                        "opponent_level" to state.opponent.level,
                        "opponent_name" to state.opponent.name
                    )
                )
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        addLog(response.data.msg)
                        applyDamage(response.data.damage, true)
                        if (!battleOver) {
                            handler.postDelayed({
                                isPlayerTurn = true
                                setFormulaButtonsEnabled(true)
                                addLog("轮到你了，选择一个公式！")
                            }, 800)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isPlayerTurn = true
                    setFormulaButtonsEnabled(true)
                }
            }
        }
    }

    private fun endBattle(playerWon: Boolean) {
        battleOver = true
        setFormulaButtonsEnabled(false)
        addLog(if (playerWon) "🎉 恭喜获胜！你的宠物变得更强大！" else "💔 战斗失败，继续加油训练吧！")

        handler.postDelayed({
            AlertDialog.Builder(this)
                .setTitle(if (playerWon) "胜利！" else "失败")
                .setMessage(if (playerWon) "你的宠物在战斗中获胜！" else "别灰心，多练练公式再来挑战！")
                .setPositiveButton("再来一局") { _, _ ->
                    binding.layoutStart.visibility = View.VISIBLE
                    binding.layoutBattle.visibility = View.GONE
                    binding.layoutLog.visibility = View.GONE
                    startBattle()
                }
                .setNegativeButton("离开") { _, _ -> finish() }
                .show()
        }, 1000)
    }

    private fun setFormulaButtonsEnabled(enabled: Boolean) {
        binding.btnFormula1.isEnabled = enabled
        binding.btnFormula2.isEnabled = enabled
        binding.btnFormula3.isEnabled = enabled
        binding.btnFormula4.isEnabled = enabled
    }

    private fun addLog(msg: String) {
        binding.tvLog.append("$msg\n")
        binding.logScroll.post {
            binding.logScroll.fullScroll(View.FOCUS_DOWN)
        }
    }
}