package com.example.practice.api

import com.example.practice.RequestBodyApi.StudentRegisterRequest
import com.example.practice.ResponsesModel.AllStudentsData
import com.example.practice.ResponsesModel.CourseStudentView
import com.example.practice.ResponsesModel.DashboardStatsResponse
import com.example.practice.ResponsesModel.DetailedStudentsResponse
import com.example.practice.ResponsesModel.FetchAllAttendanceRecord
import com.example.practice.ResponsesModel.GetRecordDataResponse
import com.example.practice.ResponsesModel.ListOfProfessor
import com.example.practice.ResponsesModel.MessageResponse
import com.example.practice.ResponsesModel.StudentDto
import com.example.practice.ResponsesModel.StudentAttendanceByCourse
import com.example.practice.ResponsesModel.StudentRegisterCsvResponse
import com.example.practice.ResponsesModel.ViewAllAttendanceRecords
import com.example.practice.ResponsesModel.ViewCourses
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @Multipart
    @POST("admin/register/csv")
    suspend fun studentRegisterCsv(
        @Header("Authorization") token: String,
        @Header("isProfessor") isProfessor: Boolean,
        @Part file: MultipartBody.Part
    ): Response<StudentRegisterCsvResponse>

    @POST("admin/create/student")
    suspend fun createStudentAccount(
        @Header("Authorization") token: String,
        @Body student: StudentRegisterRequest
    ): Response<StudentDto>

    @GET("admin/course/viewCurrent")
    suspend fun viewCurrentCourses(
        @Header("Authorization") token: String,
    ): Response<ViewCourses>

    @GET("admin/course/viewArchive")
    suspend fun viewArchivedCourses(
        @Header("Authorization") token: String,
    ): Response<ViewCourses>

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

    @GET("professor/fullattendance")
    suspend fun getFullAttendanceRecord(
        @Header("Authorization") token: String,
        @Query("joiningCode") joiningCode: String,
        @Query("batch") batch: String,
        @Query("courseName") courseName: String,
        @Query("year") year: Int,
        @Query("isArchived") isArchived: Boolean
    ): Response<FetchAllAttendanceRecord>

    @GET("professor/attendance/record")
    suspend fun getRecordData(
        @Header("Authorization") token: String,
        @Query("recordId") recordId: String,
        @Query("isArchived") isArchived: Boolean,
    ): Response<GetRecordDataResponse>

    @GET("professor/students")
    suspend fun getAllStudentsData(
        @Header("Authorization") token: String,
    ): Response<AllStudentsData>

    @GET("admin/professor/viewAll")
    suspend fun getAllProfessorData(
        @Header("Authorization") token: String,
    ): Response<ListOfProfessor>

    @GET("admin/student/attendance")
    suspend fun getStudentAttendance(
        @Header("Authorization") token: String,
        @Query("name") name: String,
        @Query("rollno") rollno: String,
    ): Response<StudentAttendanceByCourse>

    // ── New endpoints for enhanced admin dashboard ──

    @GET("admin/dashboard/stats")
    suspend fun getDashboardStats(
        @Header("Authorization") token: String
    ): Response<com.example.practice.ResponsesModel.DashboardStatsDto>

    @GET("admin/students/detailed")
    suspend fun getDetailedStudents(
        @Header("Authorization") token: String,
    ): Response<DetailedStudentsResponse>

    @DELETE("admin/student/{id}")
    suspend fun deleteStudent(
        @Header("Authorization") token: String,
        @Path("id") studentId: Int,
    ): Response<MessageResponse>

    @GET("admin/dashboard/audit-logs")
    suspend fun getAuditLogs(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("action") action: String? = null
    ): Response<com.example.practice.ResponsesModel.AuditLogsResponse>

    @GET("admin/dashboard/security")
    suspend fun getSecurityStats(
        @Header("Authorization") token: String
    ): Response<com.example.practice.ResponsesModel.SecurityStatsResponse>

    @GET("admin/students/search")
    suspend fun searchStudents(
        @Header("Authorization") token: String,
        @Query("branch") branch: String? = null,
        @Query("section") section: String? = null,
        @Query("year") year: String? = null,
        @Query("rollno") rollno: String? = null,
        @Query("name") name: String? = null
    ): Response<com.example.practice.ResponsesModel.SearchStudentResponse>
}