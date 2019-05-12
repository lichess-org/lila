package lila.security

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.joda.time.Instant
import scala.concurrent.duration.Duration

import lila.common.base.StringUtils.levenshtein

final class Flood(duration: Duration) {

  import Flood._

  private val floodNumber = 4

  private val cache: Cache[String, Messages] = Scaffeine()
    .expireAfterAccess(duration)
    .build[String, Messages]

  def filterMessage[A](uid: String, text: String)(op: => Unit): Unit =
    if (allowMessage(uid, text)) op

  def allowMessage(uid: String, text: String): Boolean = {
    val msg = Message(text, Instant.now)
    val msgs = ~cache.getIfPresent(uid)
    !duplicateMessage(msg, msgs) && !quickPost(msg, msgs) ~ {
      _ ?? cache.put(uid, msg :: msgs)
    }
  }

  private def quickPost(msg: Message, msgs: Messages): Boolean =
    msgs.lift(floodNumber) ?? (_.date isAfter msg.date.minus(10000L))
}

private[security] object Flood {

  case class Message(text: String, date: Instant)

  type Messages = List[Message]

  def duplicateMessage(msg: Message, msgs: Messages): Boolean =
    msgs.headOption ?? { m =>
      similar(m.text, msg.text) || msgs.tail.headOption.?? { m2 =>
        similar(m2.text, msg.text)
      }
    }

  private def similar(s1: String, s2: String): Boolean = {
    val distance = levenshtein(s1, s2)
    distance < 2 || distance < s1.length.min(s2.length) / 8
  }
}
