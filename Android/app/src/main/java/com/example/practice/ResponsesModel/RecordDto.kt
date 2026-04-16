package com.example.practice.ResponsesModel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecordDto(
    val __v: Int? = null,
    val _id: String? = null,
    val id: Int? = null,
    val batch: String? = null,
    val course: CourseXX? = null,
    val courseId: Int? = null,
    val date: String? = null,
    val student: List<Any>? = null,
    val sessionActive: Boolean? = null,
    val sessionStart: String? = null,
    val sessionEnd: String? = null
)

fun RecordDto.toDomain(): Record {
    return Record(
        _id = this._id ?: this.id?.toString(),
        id = this.id,
        __v = this.__v,
        batch = this.batch,
        course = this.course,
        courseId = this.courseId,
        date = this.date,
        student = this.student ?: emptyList(),
        sessionActive = this.sessionActive,
        sessionStart = this.sessionStart,
        sessionEnd = this.sessionEnd
    )
}
