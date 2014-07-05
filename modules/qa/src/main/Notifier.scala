package lila.qa

import lila.common.String._
import lila.hub.actorApi.message.LichessThread
import lila.hub.actorApi.timeline.{ Propagate, QaQuestion, QaAnswer, QaComment }
import lila.user.User

import akka.actor.ActorSelection

private[qa] final class Notifier(
    sender: String,
    messenger: ActorSelection,
    timeline: ActorSelection) {

  private[qa] def createQuestion(q: Question, u: User) {
    val msg = Propagate(QaQuestion(u.id, q.id, q.title))
    timeline ! (msg toFollowersOf u.id)
  }

  private[qa] def createAnswer(q: Question, a: Answer, u: User) {
    val msg = Propagate(QaAnswer(u.id, q.id, q.title, a.id))
    timeline ! (msg toFollowersOf u.id toUser q.userId exceptUser u.id)
    messenger ! LichessThread(
      from = sender,
      to = q.userId,
      subject = s"""${u.username} replied to your question""",
      message = s"""Your question "${q.title}" got a new answer from ${u.username}!

Check it out on ${questionUrl(q)}#answer-${a.id}""")
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
