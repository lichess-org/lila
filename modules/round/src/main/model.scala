package lila.round

import lila.game.{ Game, Pov }
import lila.user.User
import play.api.libs.json.JsObject

final class OnStart(f: GameId => Unit) extends (GameId => Unit):
  def apply(g: GameId) = f(g)

final class TellRound(f: (GameId, Any) => Unit) extends ((GameId, Any) => Unit):
  def apply(g: GameId, msg: Any) = f(g, msg)

final class IsSimulHost(f: UserId => Fu[Boolean]) extends (UserId => Fu[Boolean]):
  def apply(u: UserId) = f(u)

opaque type ScheduleExpiration = Game => Unit
object ScheduleExpiration extends TotalWrapper[ScheduleExpiration, Game => Unit]

final class IsOfferingRematch(f: Pov => Boolean) extends (Pov => Boolean):
  def apply(p: Pov) = f(p)

case class ChangeFeatured(pov: Pov, mgs: JsObject)
