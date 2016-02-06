package controllers

import play.api.mvc._

import lila.ai.actorApi.{ Chess960, KingOfTheHill, Variant }
import lila.api.Context
import lila.app._

import play.api.libs.ws.WS
import play.api.Play.current

object Ai extends LilaController {

  private def requestVariant(req: RequestHeader): Variant = Variant(~get("variant", req))

  def move = Action.async { req =>
    Env.ai.server.move(
      uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
      initialFen = get("initialFen", req),
      level = getInt("level", req) | 1,
      variant = requestVariant(req)
    ) fold (
        err => {
          logwarn("[ai] stockfish server play: " + err)
          InternalServerError("AI server failed: " + err.toString)
        },
        res => Ok(res.move)
      )
  }

  def analyse = Action.async { req =>
    get("replyUrl", req) foreach { replyToUrl =>
      Env.ai.server.analyse(
        uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
        initialFen = get("initialFen", req),
        requestedByHuman = getBool("human", req),
        variant = requestVariant(req)
      ).effectFold({
          case lila.ai.Queue.FullException => logwarn("Dropping analyse request, queue is full")
          case err                         => WS.url(s"$replyToUrl/err").post(err.toString)
        },
          infos => WS.url(replyToUrl).post(lila.analyse.Info encodeList infos)
        )
    }
    fuccess(Ok)
  }
}
