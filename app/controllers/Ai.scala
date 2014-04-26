package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._

object Ai extends LilaController {

  def move = Action.async { req =>
    Env.ai.server.move(
      uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
      initialFen = get("initialFen", req),
      level = getInt("level", req) | 1
    ) fold (
        err => {
          logwarn("[ai] stockfish server play: " + err)
          InternalServerError("AI server failed: " + err.toString)
        },
        res => Ok(res.move)
      )
  }

  def analyse = Action.async { req =>
    Env.ai.server.analyse(
      uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
      initialFen = get("initialFen", req),
      requestedByHuman = getBool("human", req)
    ) fold (
        err => {
          logwarn("[ai] stockfish server analyse: " + err)
          InternalServerError(err.toString)
        },
        infos => Ok(lila.analyse.Info encodeList infos)
      )
  }
}
