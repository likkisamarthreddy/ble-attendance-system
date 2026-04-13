package com.example.practice.api

import com.example.practice.RequestBodyApi.CourseInfo
import com.example.practice.RequestBodyApi.EndSessionRequest
import com.example.practice.RequestBodyApi.MarkManualAttendanceRequest
import com.example.practice.RequestBodyApi.ModifyAttendanceRequest
import com.example.practice.RequestBodyApi.ModifyStudentsInCourse
import com.example.practice.RequestBodyApi.UpdateGeofenceRequest
import com.example.practice.ResponsesModel.AllStudentsData
import com.example.practice.ResponsesModel.CourseStudentView
import com.example.practice.ResponsesModel.CreateAttendanceResponse
import com.example.practice.ResponsesModel.CreateCourseResponse
import com.example.practice.ResponsesModel.FetchAllAttendanceRecord
import com.example.practice.ResponsesModel.GetProfessorProfileResponse
import com.example.practice.ResponsesModel.GetRecordDataResponse
import com.example.practice.ResponsesModel.MarkManualAttendanceResponse
import com.example.practice.ResponsesModel.MessageResponse
import com.example.practice.ResponsesModel.ModifyStudentsInCoursesResponse
import com.example.practice.ResponsesModel.UpdateGeofenceResponse
import com.example.practice.ResponsesModel.ViewAllAttendanceRecords
import com.example.practice.ResponsesModel.ViewCourses
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface ProfessorApi {
    @GET("professor/course/current")
    suspend fun getProfessorCurrentCourseData(
        @Header("Authorization") token: String,
    ): Response<ViewCourses>

    @GET("professor/course/archived")
    suspend fun getProfessorArchivedCourseData(
        @Header("Authorization") token: String,
    ): Response<ViewCourses>

    @GET("professor/students")
    suspend fun getAllStudentsData(
        @Header("Authorization") token: String,
    ): Response<AllStudentsData>

    @GET("professor/course/student")
    suspend fun getStudentsInCourse(
        @Header("Authorization") token: String,
        @Query("batch") batch: String,
        @Query("courseName") courseName: String,
        @Query("isArchived") isArchived: Boolean,
        @Query("year") year: Int,
        @Query("joiningCode") joiningCode: String
    ): Response<CourseStudentView>

    @GET("professor/attendance/course")
    suspend fun getAllAttendanceRecords(
        @Header("Authorization") token: String,
        @Query("joiningCode") joiningCode: String,
        @Query("batch") batch: String,
        @Query("courseName") courseName: String,
        @Query("year") year: Int,
        @Query("isArchived") isArchived: Boolean
    ): Response<ViewAllAttendanceRecords>

    @GET("professor/attendance/record")
    suspend fun getRecordData(
        @Header("Authorization") token: String,
        @Query("recordId") recordId: String,
        @Query("isArchived") isArchived: Boolean,
    ): Response<GetRecordDataResponse>

    @GET("professor/fullattendance")
    suspend fun getFullAttendanceRecord(
        @Header("Authorization") token: String,
        @Query("joiningCode") joiningCode: String,
        @Query("batch") batch: String,
        @Query("courseName") courseName: String,
        @Query("year") year: Int,
        @Query("isArchived") isArchived: Boolean
    ): Response<FetchAllAttendanceRecord>

    @POST("professor/course")
    suspend fun createCourse(
        @Body createCourse: CourseInfo,
        @Header("Authorization") token: String
    ): Response<CreateCourseResponse>

    @POST("professor/attendance")
    suspend fun createAttendance(
        @Header("Authorization") token: String,
        @Body createAttendance: CourseInfo,
    ): Response<CreateAttendanceResponse>

    @PATCH("professor/course")
    suspend fun modifyStudentsInCourse(
        @Header("Authorization") token: String,
        @Body modifyStudents: ModifyStudentsInCourse,
    ): Response<ModifyStudentsInCoursesResponse>

    @PATCH("professor/attendance/manual")
    suspend fun markManualAttendance(
        @Header("Authorization") token: String,
        @Body markManualAttendance: MarkManualAttendanceRequest,
    ): Response<MarkManualAttendanceResponse>

    @PATCH("professor/attendance/modify")
    suspend fun modifyAttendance(
        @Header("Authorization") token: String,
        @Body modifyAttendance: ModifyAttendanceRequest,
    ): Response<MessageResponse>

    @PATCH("professor/attendance/live/modify")
    suspend fun modifyLiveAttendance(
        @Header("Authorization") token: String,
        @Body modifyAttendance: ModifyAttendanceRequest,
    ): Response<MessageResponse>

    @GET("professor/profile")
    suspend fun getProfessorProfile(
        @Header("Authorization") token: String,
    ): Response<GetProfessorProfileResponse>

    @PATCH("professor/attendance/session/end")
    suspend fun endSession(
        @Header("Authorization") token: String,
        @Body endSessionRequest: EndSessionRequest,
    ): Response<MessageResponse>

    @PATCH("professor/course/geofence")
    suspend fun updateGeofence(
        @Header("Authorization") token: String,
        @Body request: UpdateGeofenceRequest,
    ): Response<UpdateGeofenceResponse>
}
