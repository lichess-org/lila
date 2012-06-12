package lila
package security

import memo.Builder

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

final class Flood {

  private val ttl = 60 * 1000
  private val floodNumber = 4
  private val floodDelay = 10 * 1000

  case class Message(text: String, date: DateTime) {

    def same(other: Message) = this.text == other.text
  }
  type Messages = List[Message]

  // uid, [date, message]
  private val messages = Builder.expiry[String, Messages](ttl)

  def filterMessage[A](uid: String, text: String)(op: ⇒ Unit) {
    if (allowMessage(uid, text)) op
  }

  def allowMessage(uid: String, text: String): Boolean = {
    val msg = Message(text, DateTime.now)
    val msgs = ~Option(messages getIfPresent uid)
    val allow = !duplicateMessage(msg, msgs) && !quickPost(msg, msgs)
    allow ~ { a ⇒
      if (a) messages.put(uid, msg :: msgs)
    }
  }

  private def duplicateMessage(msg: Message, msgs: Messages): Boolean =
    msgs.headOption.fold(
      m ⇒ (m same msg) || msgs.tail.headOption.fold(
        _ same msg,
        false
      ),
      false)

  private def quickPost(msg: Message, msgs: Messages): Boolean =
    (msgs lift floodNumber).fold(
      old ⇒ old.date > (msg.date - floodDelay.millis),
      false
    )
}
