package controllers

import play.api.mvc._

import lila.app._
import lila.api.Context

object Ai extends LilaController {

  private def stockfishServer = Env.ai.stockfishServer
  private def isServer = Env.ai.isServer

  def playStockfish = Action.async { req =>
    IfServer {
      stockfishServer.move(
        uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
        initialFen = get("initialFen", req),
        level = getInt("level", req) | 1
      ) fold (
          err => {
            logwarn("[ai] stockfish server play: " + err)
            InternalServerError(err.toString)
          },
          res => Ok(res.move)
        )
    }
  }

  def analyseStockfish = Action.async { req =>
    IfServer {
      stockfishServer.analyse(
        uciMoves = get("uciMoves", req) ?? (_.split(' ').toList),
        initialFen = get("initialFen", req)
      ) fold (
          err => {
            logwarn("[ai] stockfish server analyse: " + err)
            InternalServerError(err.toString)
          },
          infos => Ok(lila.analyse.Info encodeList infos)
        )
    }
  }

  def loadStockfish = Action.async { req =>
    IfServer {
      stockfishServer.load map { Ok(_) }
    }
  }

  private def IfServer(result: => Fu[SimpleResult]) =
    isServer.fold(result, fuccess(BadRequest("Not an AI server")))
}
