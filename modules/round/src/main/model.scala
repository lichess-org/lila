package lila.round

import play.api.libs.json.JsObject

import lila.game.{ Game, Pov }

opaque type ScheduleExpiration = Game => Unit
object ScheduleExpiration extends FunctionWrapper[ScheduleExpiration, Game => Unit]
