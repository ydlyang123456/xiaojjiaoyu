package com.studycheck.student.network

import com.studycheck.student.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @Multipart
    @POST("api/questions/upload")
    fun uploadQuestion(
        @Part image: MultipartBody.Part,
        @Part("subject") subject: RequestBody? = null,
        @Part("description") description: RequestBody? = null
    ): Call<ApiResponse<Question>>

    @GET("api/questions/")
    fun getQuestions(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Call<ApiResponse<PaginatedResponse<Question>>>

    @GET("api/questions/{id}")
    fun getQuestion(@Path("id") id: Int): Call<ApiResponse<Question>>

    @DELETE("api/questions/{id}")
    fun deleteQuestion(@Path("id") id: Int): Call<ApiResponse<Any>>

    @Multipart
    @POST("api/search/question")
    fun searchQuestion(
        @Part image: MultipartBody.Part? = null,
        @Part("query_text") queryText: RequestBody? = null,
        @Part("subject") subject: RequestBody? = null,
        @Part("question_id") questionId: RequestBody? = null
    ): Call<ApiResponse<SearchRecord>>

    @GET("api/search/records")
    fun getSearchRecords(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("subject") subject: String? = null
    ): Call<ApiResponse<PaginatedResponse<SearchRecord>>>

    @GET("api/search/{id}")
    fun getSearchRecord(@Path("id") id: Int): Call<ApiResponse<SearchRecord>>

    @POST("api/analysis/generate")
    fun generateAnalysis(@Body body: Map<String, Any>): Call<ApiResponse<AIAnalysis>>

    @GET("api/analysis/history")
    fun getAnalysisHistory(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Call<ApiResponse<PaginatedResponse<AIAnalysis>>>

    @GET("api/analysis/{id}")
    fun getAnalysis(@Path("id") id: Int): Call<ApiResponse<AIAnalysis>>

    @POST("api/knowledge/query")
    @FormUrlEncoded
    suspend fun queryKnowledge(
        @Field("query") query: String
    ): ApiResponse<KnowledgeResult>

    @POST("api/knowledge/vocabulary")
    @FormUrlEncoded
    suspend fun queryVocabulary(
        @Field("word") word: String
    ): ApiResponse<VocabularyResult>

    @Multipart
    @POST("api/correction/upload")
    suspend fun uploadCorrection(
        @Part image: MultipartBody.Part?,
        @Part("search_record_id") recordId: Int,
        @Part("corrected_text") correctedText: RequestBody?,
        @Part("is_correct") isCorrect: RequestBody
    ): ApiResponse<Correction>

    @GET("api/correction/list/{record_id}")
    suspend fun getCorrections(
        @Path("record_id") recordId: Int
    ): ApiResponse<List<Correction>>

    @GET("api/pet/info")
    suspend fun getPetInfo(): ApiResponse<PetInfo>

    @POST("api/pet/feed")
    suspend fun feedPet(): ApiResponse<PetInfo>

    @POST("api/pet/play")
    suspend fun playWithPet(): ApiResponse<PetInfo>

    @POST("api/pet/change-skin")
    suspend fun changePetSkin(
        @Body body: Map<String, String>
    ): ApiResponse<PetInfo>

    @POST("api/pet/rename")
    suspend fun renamePet(
        @Body body: Map<String, String>
    ): ApiResponse<PetInfo>

    @GET("api/pet/leaderboard")
    suspend fun getLeaderboard(): ApiResponse<List<LeaderboardItem>>

    @POST("api/auth/bind-student")
    suspend fun bindStudent(@Body body: Map<String, String>): ApiResponse<StudentRelation>

    @GET("api/auth/my-students")
    suspend fun getMyStudents(): ApiResponse<List<StudentRelation>>

    // 宠物对战
    @GET("api/battle/formulas")
    suspend fun getBattleFormulas(): ApiResponse<List<FormulaInfo>>

    @POST("api/battle/start")
    suspend fun startBattle(): ApiResponse<BattleState>

    @POST("api/battle/attack")
    suspend fun battleAttack(@Body body: Map<String, Any>): ApiResponse<AttackResult>

    @POST("api/battle/solve")
    suspend fun battleSolve(@Body body: Map<String, Any>): ApiResponse<SolveResult>

    @POST("api/battle/opponent-attack")
    suspend fun opponentAttack(@Body body: Map<String, Any>): ApiResponse<OpponentAttackResult>

    // 版本更新
    @GET("api/app/version")
    suspend fun getAppVersion(): ApiResponse<AppVersion>
}
