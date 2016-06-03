package lila.qa

import lila.hub.actorApi.timeline.{ Propagate, QaQuestion, QaAnswer, QaComment }
import lila.notify.Notification.Notifies
import lila.notify.{Notification, NotifyApi}
import lila.notify.QaAnswer._
import lila.user.User

import akka.actor.ActorSelection

private[qa] final class Notifier( notifyApi: NotifyApi,
                                  timeline: ActorSelection) {

  private[qa] def createQuestion(q: Question, u: User) {
    val msg = Propagate(QaQuestion(u.id, q.id, q.title))
    timeline ! (msg toFollowersOf u.id)
  }

  private[qa] def createAnswer(q: Question, a: Answer, u: User) {
    val msg = Propagate(QaAnswer(u.id, q.id, q.title, a.id))
    timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
    if (u.id != q.userId) notifyAsker(q, a)
  }

  private[qa] def notifyAsker(q: Question, a: Answer) = {
      val answererId = AnswererId(a.userId)
      val questionTitle = Title(q.title)
      val questionId = QuestionId(q.id)
      val questionSlug = QuestionSlug(q.slug)
      val answerId = AnswerId(a.id)

      val notificationContent = lila.notify.QaAnswer(answererId, questionTitle, questionId, questionSlug, answerId)
      val notification = Notification(Notifies(q.userId), notificationContent)

      notifyApi.addNotification(notification)
  }

  private[qa] def createQuestionComment(q: Question, c: Comment, u: User) {
    val msg = Propagate(QaComment(u.id, q.id, q.title, c.id))
    timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
  }

  private[qa] def createAnswerComment(q: Question, a: Answer, c: Comment, u: User) {
    val msg = Propagate(QaComment(u.id, q.id, q.title, c.id))
    timeline ! (msg toFollowersOf u.id toUser a.userId exceptUser u.id)
  }

  private def questionUrl(q: Question) =
    s"http://lichess.org/qa/${q.id}/${q.slug}"
}
