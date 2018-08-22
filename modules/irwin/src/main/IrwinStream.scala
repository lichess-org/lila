package lila.irwin

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.analyse.Analysis.Analyzed
import lila.report.SuspectId

final class IrwinStream(system: ActorSystem) {

  import lila.common.HttpStream._

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  def enumerator: Enumerator[String] = {
    var stream: Option[ActorRef] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        stream = system.lilaBus.subscribeFun('irwin) {
          case req: IrwinRequest =>
            lila.mon.mod.irwin.streamEventType("request")()
            channel.push(requestJson(req))
        } some
      },
      onComplete = onComplete(stream, system)
    ) &> stringify
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
      case Analyzed(game, analysis) => Json.obj(
        "id" -> game.id,
        "white" -> game.whitePlayer.userId,
        "black" -> game.blackPlayer.userId,
        "pgn" -> game.pgnMoves.mkString(" "),
        "emts" -> game.clockHistory.isDefined ?? game.moveTimes.map(_.map(_.centis)),
        "analysis" -> analysis.infos.map { info =>
          info.cp.map { cp => Json.obj("cp" -> cp.value) } orElse
            info.mate.map { mate => Json.obj("mate" -> mate.value) } getOrElse
            JsNull
        }
      )
    }
  )
}
