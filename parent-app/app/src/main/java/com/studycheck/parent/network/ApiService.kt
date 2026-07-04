package com.studycheck.parent.network

import com.studycheck.parent.data.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/register")
    fun register(@Body body: Map<String, String>): Call<ApiResponse<LoginResponse>>

    @POST("api/auth/login")
    fun login(@Body body: Map<String, String>): Call<ApiResponse<LoginResponse>>

    @GET("api/auth/profile")
    fun getProfile(): Call<ApiResponse<User>>

    @PUT("api/auth/profile")
    fun updateProfile(@Body body: Map<String, String>): Call<ApiResponse<User>>

    @POST("api/auth/bind-student")
    fun bindStudent(@Body body: Map<String, String>): Call<ApiResponse<StudentRelation>>

    @GET("api/auth/my-students")
    fun getMyStudents(): Call<ApiResponse<List<StudentRelation>>>

    @GET("api/questions/")
    fun getQuestions(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("student_id") studentId: Int
    ): Call<ApiResponse<PaginatedResponse<Question>>>

    @GET("api/questions/{id}")
    fun getQuestion(@Path("id") id: Int): Call<ApiResponse<Question>>

    @GET("api/search/records")
    fun getSearchRecords(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("student_id") studentId: Int,
        @Query("subject") subject: String? = null
    ): Call<ApiResponse<PaginatedResponse<SearchRecord>>>

    @GET("api/search/{id}")
    fun getSearchRecord(@Path("id") id: Int): Call<ApiResponse<SearchRecord>>

    @POST("api/analysis/generate")
    fun generateAnalysis(@Body body: Map<String, Any>): Call<ApiResponse<AIAnalysis>>

    @GET("api/analysis/history")
    fun getAnalysisHistory(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("student_id") studentId: Int
    ): Call<ApiResponse<PaginatedResponse<AIAnalysis>>>

    @GET("api/analysis/{id}")
    fun getAnalysis(@Path("id") id: Int): Call<ApiResponse<AIAnalysis>>
}
