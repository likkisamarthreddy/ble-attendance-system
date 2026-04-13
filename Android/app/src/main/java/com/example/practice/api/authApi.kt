package com.example.practice.api

import com.example.practice.RequestBodyApi.ProfessorRegisterRequest
import com.example.practice.RequestBodyApi.StudentRegisterRequest
import com.example.practice.ResponsesModel.ProfessorRegisterResponse
import com.example.practice.ResponsesModel.StudentRegisterResponse
import com.example.practice.ResponsesModel.UserRoleResponses
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @GET("auth")
    suspend fun checkUserRole(
        @Header("Authorization") authHeader: String, // Pass UID securely as Bearer Token
        @Query("androidId") androidId: String,
    ): Response<UserRoleResponses>

    // Register Student
    @POST("student/register")
    suspend fun registerStudent(
        @Body studentRequest: StudentRegisterRequest,
        @Header("Authorization") authHeader: String
    ): Response<StudentRegisterResponse>

    // Register Professor
    @POST("professor/register")
    suspend fun registerProfessor(
        @Body professorRequest: ProfessorRegisterRequest,
        @Header("Authorization") authHeader: String
    ): Response<ProfessorRegisterResponse>

    @GET("student/sim")
    suspend fun bindSim(
        @Header("Authorization") authHeader: String,
        @Header("AndroidId") androidId: String,
        @Header("SimId") simId: Int,
        ): Response<Unit>
}
