package com.khemanthkota.tollywoodtycoon.empire

internal enum class StudioNoticeKind { BIRTHDAY, MOVIE_HIT, MOVIE_FAILURE, AWARD, ANNIVERSARY, SCHEDULE_CONFLICT, CONTRACT_EXPIRY, PAYMENT_DUE }
internal data class NoticeAction(val id: String, val costLakh: Int, val friendshipChange: Int) {
    init { require(id.isNotBlank() && costLakh >= 0) }
}
internal data class StudioNotice(val id: String, val personId: String, val day: Int, val kind: StudioNoticeKind, val actions: List<NoticeAction>, val responseId: String? = null) {
    init { require(id.isNotBlank() && personId.isNotBlank() && day >= 0 && actions.map { it.id }.distinct().size == actions.size) }
}
internal data class NoticeInbox(val notices: List<StudioNotice> = emptyList()) {
    fun due(day: Int) = notices.filter { it.day <= day && it.responseId == null }
    fun enqueue(notice: StudioNotice): NoticeInbox = if (notices.any { it.id == notice.id }) this else copy(notices = notices + notice)
    fun respond(id: String, actionId: String, day: Int, availableLakh: Long): Pair<NoticeInbox, NoticeAction> {
        val notice = due(day).first { it.id == id }
        val action = notice.actions.first { it.id == actionId }
        require(action.costLakh <= availableLakh)
        return copy(notices = notices.map { if (it.id == id) it.copy(responseId = actionId) else it }) to action
    }
}
