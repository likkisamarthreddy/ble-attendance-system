package com.example.practice.utils

import retrofit2.HttpException
import java.io.IOException
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("API call successful but body is empty", response.code())
            }
        } else {
            ApiResult.Error(response.errorBody()?.string() ?: "Unknown API error", response.code())
        }
    } catch (e: HttpException) {
        ApiResult.Error(e.message(), e.code())
    } catch (e: IOException) {
        ApiResult.Error("Network error: Please check your connection", null)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error", null)
    }
}
