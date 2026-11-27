package com.khemanthkota.tollywoodtycoon.empire
import org.junit.Assert.*
import org.junit.Test
class StudioNotificationsTest {
    private val notice = StudioNotice("birthday-1","actor-1",10,StudioNoticeKind.BIRTHDAY,listOf(NoticeAction("wish",0,1),NoticeAction("gift",2,3)))
    @Test fun notificationsFollowGameDays() {
        val inbox = NoticeInbox().enqueue(notice)
        assertTrue(inbox.due(9).isEmpty())
        assertEquals(listOf(notice),inbox.due(10))
    }
    @Test fun duplicateEventsAreIgnored() {
        assertEquals(1,NoticeInbox().enqueue(notice).enqueue(notice).notices.size)
    }
    @Test fun responseIsRecordedOnce() {
        val (updated, action) = NoticeInbox().enqueue(notice).respond(notice.id,"gift",10,2)
        assertEquals(2,action.costLakh)
        assertTrue(updated.due(10).isEmpty())
        assertEquals("gift",updated.notices.single().responseId)
    }
    @Test(expected=IllegalArgumentException::class) fun unaffordableGiftRejected() {
        NoticeInbox().enqueue(notice).respond(notice.id,"gift",10,1)
    }
}
