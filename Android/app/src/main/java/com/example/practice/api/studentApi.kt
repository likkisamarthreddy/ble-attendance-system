package com.example.practice.api

import com.example.practice.RequestBodyApi.FaceEmbeddingRequest
import com.example.practice.RequestBodyApi.FaceMultiEmbeddingRequest
import com.example.practice.RequestBodyApi.JoinCourseRequest
import com.example.practice.RequestBodyApi.ScanAttendanceRequest
import com.example.practice.ResponsesModel.ActiveSessionsResponse
import com.example.practice.ResponsesModel.FaceVerifyResponse
import com.example.practice.ResponsesModel.MessageResponse
import com.example.practice.ResponsesModel.ScanAttendanceResponse
import com.example.practice.ResponsesModel.StudentCourseAttendanceResponse
import com.example.practice.ResponsesModel.StudentCourseResponse
import com.example.practice.ResponsesModel.StudentProfileResponse
import com.example.practice.ResponsesModel.StudentViewAttendanceResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface StudentApi {
    @POST("student/attendance")
    suspend fun markScannedAttendance(
        @Header("Authorization") token: String,
        @Body scanAttendance: ScanAttendanceRequest
    ): Response<ScanAttendanceResponse>

    @POST("student/course/join")
    suspend fun StudentJoiningCode(
        @Header("Authorization") token: String,
        @Body joinCourseRequest: JoinCourseRequest
    ): Response<MessageResponse>

    @GET("student/courses")
    suspend fun getStudentCourses(
        @Header("Authorization") token: String,
    ): Response<StudentCourseResponse>

    @GET("student/attendance")
    suspend fun getStudentAttendance(
        @Header("Authorization") token: String,
        @Query("batch") batch: String,
        @Query("courseName") courseName: String
    ): Response<StudentViewAttendanceResponse>

    @GET("student/profile")
    suspend fun getStudentProfile(
        @Header("Authorization") token: String
    ): Response<StudentProfileResponse>

    @POST("student/face/register")
    suspend fun registerFaceEmbedding(
        @Header("Authorization") token: String,
        @Body request: FaceMultiEmbeddingRequest
    ): Response<MessageResponse>

    @POST("student/face/verify")
    suspend fun verifyFaceEmbedding(
        @Header("Authorization") token: String,
        @Body request: FaceEmbeddingRequest
    ): Response<FaceVerifyResponse>

    @GET("student/attendance/active")
    suspend fun getActiveSessions(
        @Header("Authorization") token: String
    ): Response<ActiveSessionsResponse>

    @GET("student/courses/attendance")
    suspend fun getCoursesWithAttendance(
        @Header("Authorization") token: String
    ): Response<StudentCourseAttendanceResponse>
}
