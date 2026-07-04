package com.studycheck.student.ui.pet

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studycheck.student.R
import com.studycheck.student.data.PetInfo
import com.studycheck.student.data.SkinInfo
import com.studycheck.student.databinding.ActivityPetBinding
import com.studycheck.student.network.ApiClient
import com.studycheck.student.ui.battle.BattleActivity
import com.studycheck.student.util.TouchFeedback
import kotlinx.coroutines.*

class PetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPetBinding
    private val skinIcons = mapOf(
        "default" to "🐱", "dog" to "🐶", "bunny" to "🐰", "fox" to "🦊",
        "panda" to "🐼", "unicorn" to "🦄", "dragon" to "🐲", "alien" to "👽"
    )
    private var currentPet: PetInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        TouchFeedback.applyAll(
            binding.btnFeed, binding.btnPlay, binding.btnSkins,
            binding.btnRename, binding.btnBattle
        )

        binding.btnFeed.setOnClickListener { feedPet() }
        binding.btnPlay.setOnClickListener { playWithPet() }
        binding.btnSkins.setOnClickListener { toggleSkins() }
        binding.btnRename.setOnClickListener { renamePet() }
        binding.btnBattle.setOnClickListener {
            startActivity(Intent(this, BattleActivity::class.java))
        }

        loadPetInfo()
    }

    private fun loadPetInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getPetInfo()
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        currentPet = response.data
                        updateUI(response.data)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PetActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(pet: PetInfo) {
        binding.tvPetEmoji.text = skinIcons[pet.skin] ?: "🐱"
        binding.tvPetName.text = pet.name
        binding.tvPetLevel.text = "Lv.${pet.level}"
        binding.pbExp.max = pet.max_exp
        binding.pbExp.progress = pet.exp
        binding.tvExp.text = "${pet.exp}/${pet.max_exp}"
        binding.tvHunger.text = "饱腹 ${pet.hunger}%"
        binding.tvHappiness.text = "心情 ${pet.happiness}%"
        binding.tvCorrected.text = "订正 ${pet.total_corrected}"
    }

    private fun feedPet() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.feedPet()
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                        updateUI(response.data)
                    } else {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PetActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playWithPet() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.playWithPet()
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                        updateUI(response.data)
                    } else {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PetActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleSkins() {
        val pet = currentPet ?: return
        val isHidden = binding.cardSkins.visibility == View.VISIBLE
        binding.cardSkins.visibility = if (isHidden) View.GONE else View.VISIBLE

        if (!isHidden) {
            val skins = pet.skins ?: return
            val keys = skins.keys.toList()

            binding.gvSkins.adapter = SkinAdapter(skins, pet.skin, pet.level) { skinKey ->
                changeSkin(skinKey)
            }
        }
    }

    private fun changeSkin(skinKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.changePetSkin(mapOf("skin" to skinKey))
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                        currentPet = response.data
                        updateUI(response.data)
                        binding.cardSkins.visibility = View.GONE
                        toggleSkins()
                    } else {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PetActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renamePet() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("给宠物起个新名字")
        val input = EditText(this)
        input.setText(currentPet?.name ?: "")
        builder.setView(input)
        builder.setPositiveButton("确定") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                doRename(name)
            }
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun doRename(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.renamePet(mapOf("name" to name))
                withContext(Dispatchers.Main) {
                    if (response.code == 200 && response.data != null) {
                        Toast.makeText(this@PetActivity, response.msg, Toast.LENGTH_SHORT).show()
                        currentPet = response.data
                        updateUI(response.data)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PetActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class SkinAdapter(
        private val skins: Map<String, SkinInfo>,
        private val currentSkin: String,
        private val level: Int,
        private val onClick: (String) -> Unit
    ) : BaseAdapter() {
        private val keys = skins.keys.toList()

        override fun getCount() = keys.size
        override fun getItem(position: Int) = keys[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(
                R.layout.item_skin, parent, false
            )
            val key = keys[position]
            val skin = skins[key]!!
            val canUse = level * 10 >= skin.cost

            view.findViewById<android.widget.TextView>(R.id.tvSkinIcon).text = skin.icon
            view.findViewById<android.widget.TextView>(R.id.tvSkinName).text = skin.name
            view.findViewById<android.widget.TextView>(R.id.tvSkinCost).text =
                if (skin.cost > 0) "${skin.cost}级" else "免费"

            val status = view.findViewById<android.widget.TextView>(R.id.tvSkinStatus)
            if (key == currentSkin) {
                status.text = "使用中"
                status.visibility = View.VISIBLE
            } else {
                status.visibility = View.GONE
            }

            view.alpha = if (canUse) 1f else 0.5f
            view.setOnClickListener { if (canUse) onClick(key) }

            return view
        }
    }
}
