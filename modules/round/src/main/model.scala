package lila.round

import play.api.libs.json.JsObject

import lila.game.{ Game, Pov }

opaque type OnStart = GameId => Unit
object OnStart extends FunctionWrapper[OnStart, GameId => Unit]

opaque type TellRound = (GameId, Any) => Unit
object TellRound extends FunctionWrapper[TellRound, (GameId, Any) => Unit]

opaque type IsSimulHost = UserId => Fu[Boolean]
object IsSimulHost extends FunctionWrapper[IsSimulHost, UserId => Fu[Boolean]]

opaque type ScheduleExpiration = Game => Unit
object ScheduleExpiration extends FunctionWrapper[ScheduleExpiration, Game => Unit]

opaque type IsOfferingRematch = Pov => Boolean
object IsOfferingRematch extends FunctionWrapper[IsOfferingRematch, Pov => Boolean]

case class ChangeFeatured(pov: Pov, mgs: JsObject)
