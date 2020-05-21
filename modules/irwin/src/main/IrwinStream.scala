package lidraughts.irwin

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lidraughts.analyse.Analysis.Analyzed
import lidraughts.report.SuspectId

final class IrwinStream(system: ActorSystem) {

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  private val classifier = 'irwin

  def enumerator: Enumerator[String] = {
    var subscriber: Option[lidraughts.common.Tellable] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        subscriber = system.lidraughtsBus.subscribeFun(classifier) {
          case req: IrwinRequest =>
            lidraughts.mon.mod.irwin.streamEventType("request")()
            channel.push(requestJson(req))
        } some
      },
      onComplete = subscriber foreach { system.lidraughtsBus.unsubscribe(_, classifier) }
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
      case (game, analysis) => Json.obj(
        "id" -> game.id,
        "white" -> game.whitePlayer.userId,
        "black" -> game.blackPlayer.userId,
        "pdn" -> game.pdnMoves.mkString(" "),
        "emts" -> game.clockHistory.isDefined ?? game.moveTimes.map(_.map(_.centis)),
        "analysis" -> analysis.map {
          _.infos.map { info =>
            info.cp.map { cp => Json.obj("cp" -> cp.value) } orElse
              info.win.map { win => Json.obj("win" -> win.value) } getOrElse
              JsNull
          }
        }
      )
    }
  )
}
