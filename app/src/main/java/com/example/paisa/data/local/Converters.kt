package com.example.paisa.data.local

import androidx.room.TypeConverter
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.DEBIT)

    @TypeConverter
    fun fromTransactionSource(source: TransactionSource): String = source.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource =
        runCatching { TransactionSource.valueOf(value) }.getOrDefault(TransactionSource.MANUAL)

    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus =
        runCatching { TransactionStatus.valueOf(value) }.getOrDefault(TransactionStatus.CONFIRMED)
}
