package com.studycheck.parent.data

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

data class StudentRelation(
    val id: Int,
    val parent_id: Int,
    val student_id: Int,
    val relation_type: String,
    val student: User?
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
