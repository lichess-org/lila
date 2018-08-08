package lidraughts.qa

import lidraughts.hub.actorApi.timeline.{ Propagate, QaQuestion, QaAnswer, QaComment }
import lidraughts.notify.Notification.Notifies
import lidraughts.notify.{ Notification, NotifyApi }
import lidraughts.user.User

import akka.actor.ActorSelection

private[qa] final class Notifier(
    notifyApi: NotifyApi,
    timeline: ActorSelection
) {

  private[qa] def createQuestion(q: Question, u: User): Unit = {
    val msg = Propagate(QaQuestion(u.id, q.id, q.title))
    timeline ! (msg toFollowersOf u.id)
  }

  private[qa] def createAnswer(q: Question, a: Answer, u: User): Unit = {
    val msg = Propagate(QaAnswer(u.id, q.id, q.title, a.id))
    timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
    if (u.id != q.userId) notifyAsker(q, a)
  }

  private[qa] def notifyAsker(q: Question, a: Answer) = {
    import lidraughts.notify.QaAnswer
    import lidraughts.common.String.shorten

    val answererId = QaAnswer.AnswererId(a.userId)
    val question = QaAnswer.Question(id = q.id, slug = q.slug, title = shorten(q.title, 80))
    val answerId = QaAnswer.AnswerId(a.id)

    val notificationContent = QaAnswer(answererId, question, answerId)
    val notification = Notification.make(Notifies(q.userId), notificationContent)

    notifyApi.addNotification(notification)
  }

  private[qa] def createQuestionComment(q: Question, c: Comment, u: User): Unit = {
    val msg = Propagate(QaComment(u.id, q.id, q.title, c.id))
    timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
  }

  private[qa] def createAnswerComment(q: Question, a: Answer, c: Comment, u: User): Unit = {
    val msg = Propagate(QaComment(u.id, q.id, q.title, c.id))
    timeline ! (msg toFollowersOf u.id toUser a.userId exceptUser u.id)
  }
}
