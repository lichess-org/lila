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
    !quickPost(msg, msgs) ~ {
      _ ?? cache.put(uid, msg :: msgs)
    }
  }

  private def quickPost(msg: Message, msgs: Messages): Boolean =
    msgs.lift(floodNumber) ?? (_.date isAfter msg.date.minus(10000L))
}

private object Flood {

  private[security] case class Message(text: String, date: Instant)

  private type Messages = List[Message]
}
