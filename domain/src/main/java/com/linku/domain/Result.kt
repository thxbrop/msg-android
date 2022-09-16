package com.linku.domain

import android.util.Log
import androidx.annotation.Keep
import com.linku.domain.extension.debug
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
@Keep
@Deprecated("Consider using [SerializableResult] instead")
data class Result<out T>(
    private val data: T? = null,
    private val code: String = "?",
    @SerialName("msg")
    val message: String? = null
) {
    private val isSuccess get() = code.trim() == "00000"

    fun peek() = data!!
    fun peekOrNull() = data

    suspend fun handle(block: suspend (T) -> Unit): Result<T> {
        if (isSuccess && data != null) block.invoke(data)
        return this
    }

    suspend fun handleUnit(block: suspend (Unit) -> Unit): Result<T> {
        if (isSuccess) block.invoke(Unit)
        return this
    }

    suspend fun catch(block: suspend (String, String) -> Unit): Result<T> {
        if (!isSuccess) block.invoke(message ?: "Unknown Error", code)
        return this
    }

    fun <R> map(converter: (T) -> R): Result<R> {
        if (data == null) return Result(
            code = code,
            message = message
        )
        return Result(
            data = converter(data),
            code = code,
            message = message
        )
    }

}

@Deprecated("Consider using [runCatching] instead")
inline fun <T> sandbox(block: () -> Result<T>): Result<T> {
    return try {
        block.invoke()
    } catch (e: SerializationException) {
        debug { Log.e("SerializationException", "", e) }
        Result(
            code = "反序列化错误",
            message = "请将应用升级至最新版本再试！"
        )
    } catch (e: Exception) {
        debug { Log.e("Result Wrapper", "sandbox:", e) }
        Result(
            code = "?",
            message = e.message ?: "sandbox exception"
        )
    }
}

