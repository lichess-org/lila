package lila.irwin

import akka.stream.scaladsl._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.Bus

final class IrwinStream {

  private val channel = "irwin"

  private val keepAliveMsg = "{\"keepAlive\":true}\n"

  private val blueprint =
    Source
      .queue[IrwinRequest](64, akka.stream.OverflowStrategy.dropHead)
      .map(requestJson)
      .map { js =>
        s"${Json.stringify(js)}\n"
      }
      .keepAlive(60.seconds, () => keepAliveMsg)

  def apply(): Source[String, _] =
    blueprint mapMaterializedValue { queue =>
      val sub = Bus.subscribeFun(channel) { case req: IrwinRequest =>
        lila.mon.mod.irwin.streamEventType("request").increment()
        queue.offer(req).unit
      }

      queue.watchCompletion() dforeach { _ =>
        Bus.unsubscribe(sub, channel)
      }
    }

  private def requestJson(req: IrwinRequest) =
    Json.obj(
      "t"      -> "request",
      "origin" -> req.origin.key,
      "user" -> Json.obj(
        "id"     -> req.suspect.user.id,
        "titled" -> req.suspect.user.hasTitle,
        "engine" -> req.suspect.user.marks.engine,
        "games"  -> req.suspect.user.count.rated
      ),
      "games" -> req.games.map { case (game, analysis) =>
        Json.obj(
          "id"    -> game.id,
          "white" -> game.whitePlayer.userId,
          "black" -> game.blackPlayer.userId,
          "pgn"   -> game.pgnMoves.mkString(" "),
          "emts"  -> game.clockHistory.isDefined ?? game.moveTimes.map(_.map(_.centis)),
          "analysis" -> analysis.map {
            _.infos.map { info =>
              info.cp.map { cp =>
                Json.obj("cp" -> cp.value)
              } orElse
                info.mate.map { mate =>
                  Json.obj("mate" -> mate.value)
                } getOrElse
                JsNull
            }
          }
        )
      }
    )
}
