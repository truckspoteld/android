package com.truckspot.newclasses

sealed class BaseResult<out T: Any> {
    data class Success<out T : Any>(val data: T) : BaseResult<T>()
    data class Error(val errorResponse: ErrorResponse) : BaseResult<Nothing>()
}