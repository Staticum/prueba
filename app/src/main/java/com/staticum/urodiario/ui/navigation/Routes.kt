package com.staticum.urodiario.ui.navigation

object Routes {
    const val HISTORY = "history"
    const val REPORTS = "reports"
    const val RECORD_FORM = "record_form?recordId={recordId}"
    const val RECORD_DETAIL = "record_detail/{recordId}"

    fun recordForm(recordId: Long? = null): String =
        if (recordId == null) "record_form" else "record_form?recordId=$recordId"

    fun recordDetail(recordId: Long): String = "record_detail/$recordId"
}
