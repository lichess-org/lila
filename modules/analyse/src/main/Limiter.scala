package lila.analyse

import scala.concurrent.duration._
import scala.collection.JavaConversions._

import lila.memo.Builder

private[analyse] final class Limiter {

  private type IP = String
  private type UserId = String
  private type GameId = String

  private case class Entry(user: UserId, ip: IP)

  private val entries = Builder.expiry[GameId, Entry](10 minutes)

  def apply(gameId: GameId, user: UserId, ip: IP): Boolean =
    if (entries.asMap.values.toList.exists {
      case Entry(u, i) => (user != "lichess" && user == u) || ip == i
    }) false
    else {
      entries.put(gameId, Entry(user, ip))
      true
    }

  def release(gameId: GameId) = entries invalidate gameId
}
