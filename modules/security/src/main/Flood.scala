package lila.security

import scala.concurrent.duration.Duration

import org.joda.time.DateTime

final class Flood(duration: Duration) {

  private val floodNumber = 4

  case class Message(text: String, date: DateTime) {

    def same(other: Message) =
      this.text.toLowerCase == other.text.toLowerCase
  }
  type Messages = List[Message]

  private val messages = lila.memo.Builder.expiry[String, Messages](duration)

  def filterMessage[A](uid: String, text: String)(op: => Unit) {
    if (allowMessage(uid, text)) op
  }

  def allowMessage(uid: String, text: String): Boolean = {
    val msg = Message(text, DateTime.now)
    val msgs = ~Option(messages getIfPresent uid)
    !duplicateMessage(msg, msgs) && !quickPost(msg, msgs) ~ {
      _ ?? messages.put(uid, msg :: msgs)
    }
  }

  private def duplicateMessage(msg: Message, msgs: Messages): Boolean =
    msgs.headOption ?? { m =>
      (m same msg) || (msgs.tail.headOption ?? (_ same msg))
    }

  private def quickPost(msg: Message, msgs: Messages): Boolean =
    msgs.lift(floodNumber) ?? (_.date isAfter msg.date.minusSeconds(10))
}
