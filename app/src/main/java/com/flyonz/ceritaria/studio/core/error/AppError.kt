package com.flyonz.ceritaria.studio.core.error

sealed interface AppError {
    data object Configuration : AppError
    data object Authentication : AppError
    data object Authorization : AppError
    data object Network : AppError
    data class Validation(val message: String) : AppError
    data object Unknown : AppError
}
