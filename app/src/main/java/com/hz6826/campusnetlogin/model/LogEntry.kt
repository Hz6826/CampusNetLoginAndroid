package com.hz6826.campusnetlogin.model

/** 一条运行记录，level 对齐原项目：INFO / STEP / SUCCESS / WARN / ERROR / HTTP。 */
data class LogEntry(val level: String, val message: String)
