package com.studycheck.student.ui.knowledge

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.studycheck.student.R
import com.studycheck.student.data.VocabularyResult
import com.studycheck.student.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KnowledgeVocabularyFragment : Fragment() {

    private var rootView: View? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentWord: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_knowledge_vocabulary, container, false)
            initViews()
        }
        return rootView
    }

    private fun initViews() {
        val view = rootView ?: return

        val etWord = view.findViewById<TextInputEditText>(R.id.etWord)
        val btnQuery = view.findViewById<MaterialButton>(R.id.btnQueryWord)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressWord)
        val cardResult = view.findViewById<CardView>(R.id.cardWordResult)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyStateWord)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupWords)

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.setOnClickListener {
                val word = chip.text.toString()
                etWord.setText(word)
                queryWord(word, progressBar, cardResult, emptyState)
            }
        }

        btnQuery.setOnClickListener {
            val word = etWord.text.toString().trim()
            if (word.isEmpty()) {
                Toast.makeText(requireContext(), "请输入单词", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            queryWord(word, progressBar, cardResult, emptyState)
        }

        view.findViewById<MaterialButton>(R.id.btnPlayWord)?.setOnClickListener {
            if (currentWord.isNotEmpty()) {
                playWord(currentWord)
            }
        }

        view.findViewById<MaterialButton>(R.id.btnPlayExample)?.setOnClickListener {
            val tvExampleEn = view.findViewById<TextView>(R.id.tvExampleEn)
            val text = tvExampleEn?.text?.toString()
            if (!text.isNullOrEmpty()) {
                playWord(text)
            }
        }
    }

    private fun queryWord(
        word: String,
        progressBar: ProgressBar,
        cardResult: CardView,
        emptyState: LinearLayout
    ) {
        progressBar.visibility = View.VISIBLE
        cardResult.visibility = View.GONE
        emptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.queryVocabulary(word)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.code == 200 && response.data != null) {
                        currentWord = word
                        showResult(response.data)
                        cardResult.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), response.msg ?: "查询失败", Toast.LENGTH_SHORT).show()
                        emptyState.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                    emptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showResult(data: VocabularyResult) {
        val view = rootView ?: return

        view.findViewById<TextView>(R.id.tvWord)?.text = data.word
        view.findViewById<TextView>(R.id.tvPhonetic)?.text = data.phonetic ?: ""
        view.findViewById<TextView>(R.id.tvPartOfSpeech)?.text = data.part_of_speech ?: ""
        view.findViewById<TextView>(R.id.tvMeaningCn)?.text = data.meaning_cn ?: ""
        view.findViewById<TextView>(R.id.tvMeaningEn)?.text = data.meaning_en ?: ""
        view.findViewById<TextView>(R.id.tvExampleEn)?.text = data.example_en ?: ""
        view.findViewById<TextView>(R.id.tvExampleCn)?.text = data.example_cn ?: ""

        val forms = buildString {
            data.plural_form?.let { append("复数: $it\n") }
            data.past_tense?.let { append("过去式: $it\n") }
            data.past_participle?.let { append("过去分词: $it") }
        }.trim()

        val tvFormsLabel = view.findViewById<TextView>(R.id.tvFormsLabel)
        val tvForms = view.findViewById<TextView>(R.id.tvForms)
        if (forms.isNotEmpty()) {
            tvFormsLabel?.visibility = View.VISIBLE
            tvForms?.visibility = View.VISIBLE
            tvForms?.text = forms
        } else {
            tvFormsLabel?.visibility = View.GONE
            tvForms?.visibility = View.GONE
        }

        val chipSynonyms = view.findViewById<ChipGroup>(R.id.chipSynonyms)
        val tvSynonymsLabel = view.findViewById<TextView>(R.id.tvSynonymsLabel)
        chipSynonyms?.removeAllViews()
        if (!data.synonyms.isNullOrEmpty()) {
            tvSynonymsLabel?.visibility = View.VISIBLE
            chipSynonyms?.visibility = View.VISIBLE
            data.synonyms.forEach { word ->
                val chip = Chip(requireContext()).apply {
                    text = word
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setOnClickListener {
                        val etWord = rootView?.findViewById<TextInputEditText>(R.id.etWord)
                        val pb = rootView?.findViewById<ProgressBar>(R.id.progressWord)
                        val cr = rootView?.findViewById<CardView>(R.id.cardWordResult)
                        val es = rootView?.findViewById<LinearLayout>(R.id.emptyStateWord)
                        etWord?.setText(text)
                        if (pb != null && cr != null && es != null) {
                            queryWord(text.toString(), pb, cr, es)
                        }
                    }
                }
                chipSynonyms?.addView(chip)
            }
        } else {
            tvSynonymsLabel?.visibility = View.GONE
            chipSynonyms?.visibility = View.GONE
        }

        val chipAntonyms = view.findViewById<ChipGroup>(R.id.chipAntonyms)
        val tvAntonymsLabel = view.findViewById<TextView>(R.id.tvAntonymsLabel)
        chipAntonyms?.removeAllViews()
        if (!data.antonyms.isNullOrEmpty()) {
            tvAntonymsLabel?.visibility = View.VISIBLE
            chipAntonyms?.visibility = View.VISIBLE
            data.antonyms.forEach { word ->
                val chip = Chip(requireContext()).apply {
                    text = word
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setOnClickListener {
                        val etWord = rootView?.findViewById<TextInputEditText>(R.id.etWord)
                        val pb = rootView?.findViewById<ProgressBar>(R.id.progressWord)
                        val cr = rootView?.findViewById<CardView>(R.id.cardWordResult)
                        val es = rootView?.findViewById<LinearLayout>(R.id.emptyStateWord)
                        etWord?.setText(text)
                        if (pb != null && cr != null && es != null) {
                            queryWord(text.toString(), pb, cr, es)
                        }
                    }
                }
                chipAntonyms?.addView(chip)
            }
        } else {
            tvAntonymsLabel?.visibility = View.GONE
            chipAntonyms?.visibility = View.GONE
        }

        val tvCollocationsLabel = view.findViewById<TextView>(R.id.tvCollocationsLabel)
        val tvCollocations = view.findViewById<TextView>(R.id.tvCollocations)
        if (!data.collocations.isNullOrEmpty()) {
            tvCollocationsLabel?.visibility = View.VISIBLE
            tvCollocations?.visibility = View.VISIBLE
            tvCollocations?.text = data.collocations.joinToString("\n") { "• $it" }
        } else {
            tvCollocationsLabel?.visibility = View.GONE
            tvCollocations?.visibility = View.GONE
        }
    }

    private fun playWord(text: String) {
        val baseUrl = ApiClient.BASE_URL
        val url = "${baseUrl}api/knowledge/tts?text=${java.net.URLEncoder.encode(text, "UTF-8")}&voice=Mia"

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(requireContext(), "语音播放失败", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "语音播放失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
