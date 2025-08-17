package lila.irwin

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.common.Json.given
import lila.game.GameExt.moveTimes

final class IrwinStream:

  private val keepAliveMsg = "{\"keepAlive\":true}\n"

  private val blueprint =
    Source
      .queue[IrwinRequest](64, akka.stream.OverflowStrategy.dropHead)
      .map(requestJson)
      .map: js =>
        s"${Json.stringify(js)}\n"
      .keepAlive(60.seconds, () => keepAliveMsg)

  def apply()(using Executor): Source[String, ?] =
    blueprint.mapMaterializedValue { queue =>
      val sub = Bus.sub[IrwinRequest]: req =>
        lila.mon.mod.irwin.streamEventType("request").increment()
        queue.offer(req)
      queue
        .watchCompletion()
        .addEffectAnyway:
          Bus.unsub[IrwinRequest](sub)
    }

  private def requestJson(req: IrwinRequest) =
    Json.obj(
      "t" -> "request",
      "origin" -> req.origin.key,
      "user" -> Json.obj(
        "id" -> req.suspect.user.id,
        "titled" -> req.suspect.user.hasTitle,
        "engine" -> req.suspect.user.marks.engine,
        "games" -> req.suspect.user.count.rated
      ),
      "games" -> req.games.map: (game, analysis) =>
        val moveTimes = game.clockHistory.isDefined.so(game.moveTimes.map(_.map(_.centis)))
        Json.obj(
          "id" -> game.id,
          "white" -> game.whitePlayer.userId,
          "black" -> game.blackPlayer.userId,
          "pgn" -> game.sans.mkString(" "),
          "emts" -> moveTimes,
          "analysis" -> analysis.map {
            _.infos.map { info =>
              info.cp
                .map { cp =>
                  Json.obj("cp" -> cp.value)
                }
                .orElse(info.mate.map { mate =>
                  Json.obj("mate" -> mate.value)
                })
                .getOrElse(JsNull)
            }
          }
        )
    )
