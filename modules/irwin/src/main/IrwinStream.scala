package lila.irwin

import akka.stream.scaladsl._
import play.api.libs.json._

import lila.common.Bus

final class IrwinStream {

  private val classifier = "userSignup"

  private val blueprint =
    Source.queue[IrwinRequest](32, akka.stream.OverflowStrategy.dropHead)
      .map(requestJson)
      .map { js => s"${Json.stringify(js)}\n" }

  def apply(): Source[String, _] = blueprint mapMaterializedValue { queue =>

    val sub = Bus.subscribeFun(classifier) {
      case req: IrwinRequest =>
        lila.mon.mod.irwin.streamEventType("request")()
        queue offer req
    }

    queue.watchCompletion foreach { _ => Bus.unsubscribe(sub, classifier) }
  }

  private def requestJson(req: IrwinRequest) = Json.obj(
    "t" -> "request",
    "origin" -> req.origin.key,
    "user" -> Json.obj(
      "id" -> req.suspect.user.id,
      "titled" -> req.suspect.user.hasTitle,
      "engine" -> req.suspect.user.engine,
      "games" -> req.suspect.user.count.rated
    ),
    "games" -> req.games.map {
      case (game, analysis) => Json.obj(
        "id" -> game.id,
        "white" -> game.whitePlayer.userId,
        "black" -> game.blackPlayer.userId,
        "pgn" -> game.pgnMoves.mkString(" "),
        "emts" -> game.clockHistory.isDefined ?? game.moveTimes.map(_.map(_.centis)),
        "analysis" -> analysis.map {
          _.infos.map { info =>
            info.cp.map { cp => Json.obj("cp" -> cp.value) } orElse
              info.mate.map { mate => Json.obj("mate" -> mate.value) } getOrElse
              JsNull
          }
        }
      )
    }
  )
}
