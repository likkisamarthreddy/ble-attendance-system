package com.example.practice.ResponsesModel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudentDto(
    val _id: String? = null,
    val id: Int? = null,
    val name: String? = null,
    val rollno: Int? = null,
    val email: String? = null,
    val courses: List<String>? = null,
    val uid: String? = null,
    val batch: List<Any>? = null,
    val __v: Int? = null,
    val attendancePercentage: String? = null
)

fun StudentDto.toDomain(): Student {
    return Student(
        _id = this._id ?: this.id?.toString() ?: "", // Allow some flexibility for dual IDs
        id = this.id, // Sometimes id is returned as int
        name = requireNotNull(this.name) { "Student name missing from API" },
        rollno = this.rollno ?: 0, // Fallback safe int
        email = requireNotNull(this.email) { "Student email missing from API" },
        courses = this.courses ?: emptyList(),
        uid = this.uid ?: "",
        batch = this.batch ?: emptyList(),
        attendancePercentage = this.attendancePercentage ?: "0"
    )
}
