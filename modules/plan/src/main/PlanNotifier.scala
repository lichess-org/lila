package lila.plan

import lila.hub.actorApi.timeline.{ Propagate }
import lila.notify.Notification.Notifies
import lila.notify.{ Notification, NotifyApi }
import lila.user.User

import akka.actor.ActorSelection

private[plan] final class PlanNotifier(
    notifyApi: NotifyApi,
    timeline: ActorSelection) {

  def onStart(user: User) =
    notifyApi.addNotification(Notification(
      Notifies(user.id),
      lila.notify.PlanStart(user.id)
    ))

  def onExpire(user: User) =
    notifyApi.addNotification(Notification(
      Notifies(user.id),
      lila.notify.PlanExpire(user.id)
    ))

  // private[qa] def createQuestion(q: Question, u: User) {
  //   val msg = Propagate(QaQuestion(u.id, q.id, q.title))
  //   timeline ! (msg toFollowersOf u.id)
  // }

  // private[qa] def createAnswer(q: Question, a: Answer, u: User) {
  //   val msg = Propagate(QaAnswer(u.id, q.id, q.title, a.id))
  //   timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
  //   if (u.id != q.userId) notifyAsker(q, a)
  // }

  // private[qa] def notifyAsker(q: Question, a: Answer) = {
  //   import lila.notify.QaAnswer
  //   import lila.common.String.shorten

  //   val answererId = QaAnswer.AnswererId(a.userId)
  //   val question = QaAnswer.Question(id = q.id, slug = q.slug, title = shorten(q.title, 80))
  //   val answerId = QaAnswer.AnswerId(a.id)

  //   val notificationContent = QaAnswer(answererId, question, answerId)
  //   val notification = Notification(Notifies(q.userId), notificationContent)

  //   notifyApi.addNotification(notification)
  // }

  // private[qa] def createQuestionComment(q: Question, c: Comment, u: User) {
  //   val msg = Propagate(QaComment(u.id, q.id, q.title, c.id))
  //   timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
  // }

  // private[qa] def createAnswerComment(q: Question, a: Answer, c: Comment, u: User) {
  //   val msg = Propagate(QaComment(u.id, q.id, q.title, c.id))
  //   timeline ! (msg toFollowersOf u.id toUser a.userId exceptUser u.id)
  // }
}
