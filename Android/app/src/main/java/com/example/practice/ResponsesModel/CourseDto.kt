package com.example.practice.ResponsesModel

data class CourseDto(
    val _id: String? = null,
    val id: Int? = null,
    val __v: Int? = null,
    val name: String? = null,
    val batch: String? = null,
    val year: Int? = null,
    val professor: String? = null,
    val students: List<String>? = null,
    val courseStatus: String? = null,
    val courseExpiry: String? = null,
    val joiningCode: String? = null
)

fun CourseDto.toDomain(): Course {
    return Course(
        _id = this._id ?: this.id?.toString() ?: "", // Allow some flexibility for dual IDs, but enforce non-null string
        name = requireNotNull(this.name) { "Course name missing from API" },
        batch = requireNotNull(this.batch) { "Course batch missing from API" },
        year = this.year ?: 0,
        professor = this.professor ?: "",
        students = this.students ?: emptyList(),
        courseStatus = this.courseStatus ?: "",
        courseExpiry = this.courseExpiry ?: "",
        joiningCode = requireNotNull(this.joiningCode) { "Course joining code missing from API" }
    )
}
