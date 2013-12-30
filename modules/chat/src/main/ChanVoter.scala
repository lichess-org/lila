package lila.chat

import scala.concurrent.duration._

private[chat] final class ChanVoter {

  private type UserId = String
  private type ChanKey = String

  private val cache = lila.memo.Builder.expiry[UserId, List[ChanKey]](1.hour)

  def apply(userId: UserId, chanKey: ChanKey) {
    cache.put(userId, chanKey :: votedKeysOf(userId).filterNot(chanKey==))
  }

  def lessVoted(userId: UserId, in: Seq[ChanKey]): Option[ChanKey] = {
    val votedKeys = votedKeysOf(userId)
    (in map { key ⇒
      key -> (votedKeys indexOf key match {
        case -1 ⇒ Int.MaxValue
        case x  ⇒ x
      })
    } sortBy (-_._2)).headOption.map(_._1)
  }

  private def votedKeysOf(userId: UserId): List[ChanKey] =
    ~Option(cache getIfPresent userId)
}
