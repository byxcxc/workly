package com.workly.app.domain

/** Carries a [WorklyError] up to the ViewModel so the UI can show a friendly message. */
class WorklyException(val error: WorklyError) : Exception(error.name)
