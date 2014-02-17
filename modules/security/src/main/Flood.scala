package lila.security

import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

final class Flood(duration: Duration) {

  private val floodNumber = 4
  private val floodDelay = 10 * 1000

  case class Message(text: String, date: DateTime) {

    def same(other: Message) = this.text == other.text
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
    msgs.lift(floodNumber) ?? (_.date > (msg.date - floodDelay.millis))
}
