package com.wiva.android.data.payment.aqsi

/** Исход операции для диагностики (только нечувствительные поля). */
enum class AqsiDiagnosticOutcome {
    SUCCESS,
    ERROR,
    APPROVED,
    DECLINED,
    CANCELLED,
}

/** Тип последней операции aQsi в памяти процесса. */
enum class AqsiDiagnosticOperationKind {
    TCP_TEST,
    PAYMENT,
    CANCEL,
}

/**
 * Сводка последней операции для вкладки диагностики (без сырого payload / PAN).
 * Обновляется из [AqsiRepositoryImpl]; провайдер singleton — task-04.
 */
data class AqsiLastOperationSummary(
    val timestampMillis: Long,
    val operationKind: AqsiDiagnosticOperationKind,
    val outcome: AqsiDiagnosticOutcome,
 /** Краткий код ответа протокола или вида ошибки (безопасный для UI). */
    val detailCode: String = "",
)

/** In-memory holder сводки последней операции aQsi (UC-5). */
class AqsiLastOperationSnapshotHolder {
    private val lock = Any()
    private var snapshot: AqsiLastOperationSummary? = null

    fun update(summary: AqsiLastOperationSummary) {
        synchronized(lock) {
            snapshot = summary
        }
    }

    fun getSnapshot(): AqsiLastOperationSummary? =
        synchronized(lock) {
            snapshot
        }
}
