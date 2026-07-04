package com.studycheck.student.data

data class User(
    val id: Int,
    val username: String,
    val role: String,
    val nickname: String?,
    val avatar: String?,
    val grade: String?,
    val created_at: String?
)

data class Question(
    val id: Int,
    val student_id: Int,
    val image_url: String,
    val subject: String?,
    val description: String?,
    val status: String,
    val created_at: String?,
    val search_records: List<SearchRecord>?
)

data class SearchRecord(
    val id: Int,
    val student_id: Int,
    val question_id: Int?,
    val query_text: String?,
    val query_image_url: String?,
    val subject: String?,
    val ai_answer: String?,
    val ai_solution: String?,
    val knowledge_points: String?,
    val difficulty: String?,
    val created_at: String?
)

data class AIAnalysis(
    val id: Int,
    val student_id: Int,
    val parent_id: Int?,
    val analysis_type: String,
    val overall_evaluation: String?,
    val subject_analysis: String?,
    val weak_points: String?,
    val strong_points: String?,
    val suggestions: String?,
    val study_habits: String?,
    val statistics_data: String?,
    val start_date: String?,
    val end_date: String?,
    val created_at: String?
)

data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T?
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val per_page: Int,
    val pages: Int
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class KnowledgeQuery(
    val query: String
)

data class KnowledgeResult(
    val query: String,
    val subject: String?,
    val content: String,
    val source: String
)

data class VocabularyResult(
    val word: String,
    val phonetic: String?,
    val part_of_speech: String?,
    val meaning_cn: String?,
    val meaning_en: String?,
    val example_en: String?,
    val example_cn: String?,
    val plural_form: String?,
    val past_tense: String?,
    val past_participle: String?,
    val synonyms: List<String>?,
    val antonyms: List<String>?,
    val collocations: List<String>?
)

data class Correction(
    val id: Int,
    val student_id: Int,
    val search_record_id: Int,
    val corrected_image_url: String?,
    val corrected_text: String?,
    val is_correct: Boolean,
    val feedback: String?,
    val created_at: String?
)

data class CorrectionUpload(
    val search_record_id: Int,
    val corrected_text: String?,
    val is_correct: Boolean
)

data class PetInfo(
    val id: Int,
    val student_id: Int,
    val name: String,
    val pet_type: String,
    val skin: String,
    val level: Int,
    val exp: Int,
    val max_exp: Int,
    val hunger: Int,
    val happiness: Int,
    val total_corrected: Int,
    val streak_days: Int,
    val last_feed_date: String?,
    val created_at: String?,
    val skins: Map<String, SkinInfo>?
)

data class SkinInfo(
    val name: String,
    val icon: String,
    val cost: Int
)

data class LeaderboardItem(
    val rank: Int,
    val pet_name: String,
    val pet_skin: String,
    val level: Int,
    val exp: Int,
    val total_corrected: Int,
    val student_name: String
)

data class StudentRelation(
    val id: Int,
    val parent_id: Int,
    val student_id: Int,
    val relation_type: String,
    val student: User?
)

// 宠物对战
data class FormulaInfo(
    val id: String,
    val name: String,
    val formula: String,
    val subject: String,
    val damage: Int,
    val difficulty: String,
    val mastery: Int,
    val correct_count: Int,
    val total_attempts: Int
)

data class BattlePlayer(
    val pet_name: String,
    val pet_skin: String,
    val level: Int,
    val hp: Int,
    val max_hp: Int
)

data class BattleOpponent(
    val name: String,
    val emoji: String,
    val level: Int,
    val hp: Int,
    val max_hp: Int
)

data class BattleState(
    val player: BattlePlayer,
    val opponent: BattleOpponent,
    val round: Int,
    val log: List<String>
)

data class AttackResult(
    val auto: Boolean,
    val formula: FormulaInfo?,
    val damage: Int?,
    val problem: ProblemInfo?,
    val msg: String
)

data class ProblemInfo(
    val question: String,
    val answer: String,
    val hint: String,
    val formula_name: String,
    val formula: String
)

data class SolveResult(
    val correct: Boolean,
    val formula: FormulaInfo?,
    val damage: Int,
    val expected: String?,
    val msg: String,
    val mastery: Int,
    val pet_exp: Int,
    val pet_level: Int
)

data class OpponentAttackResult(
    val formula_name: String,
    val formula: String,
    val damage: Int,
    val msg: String
)

// 版本更新
data class AppVersion(
    val version_code: Int,
    val version_name: String,
    val changelog: String,
    val download_url: String,
    val force_update: Boolean
)
