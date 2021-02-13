package lila.security

import com.github.blemale.scaffeine.Cache
import org.joda.time.Instant
import scala.concurrent.duration.FiniteDuration

import lila.common.base.Levenshtein.isLevenshteinDistanceLessThan
import lila.user.User

final class Flood(duration: FiniteDuration) {

  import Flood._

  private val floodNumber = 4

  private val cache: Cache[User.ID, Messages] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(duration)
    .build[User.ID, Messages]()

  def allowMessage(uid: User.ID, text: String): Boolean = {
    val msg  = Message(text, Instant.now)
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
    isLevenshteinDistanceLessThan(s1, s2, (s1.length.min(s2.length) >> 3) atLeast 2)
  }
}
