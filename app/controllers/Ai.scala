package controllers

import lila.app._
import lila.user.Context

import play.api.mvc._

object Ai extends LilaController {

  private def stockfishServer = Env.ai.stockfishServer
  private def isServer = Env.ai.isServer

  def playStockfish = Action { req ⇒
    IfServer {
      stockfishServer.play(
        pgn = ~get("pgn", req),
        initialFen = get("initialFen", req),
        level = getInt("level", req) | 1
      ) fold (
          err ⇒ {
            logwarn("[ai] stochfish server play: " + err)
            InternalServerError
          },
          Ok(_)
        )
    }
  }

  def analyseStockfish = Action { req ⇒
    IfServer {
      stockfishServer.analyse(
        pgn = ~get("pgn", req),
        initialFen = get("initialFen", req)
      ) fold (
          err ⇒ {
            logwarn("[ai] stochfish server analyse: " + err)
            InternalServerError
          },
          analyse ⇒ Ok(analyse("fakeid").encodeInfos)
        )
    }
  }

  private def IfServer(result: ⇒ Fu[Result]) =
    isServer.fold(Async(result), BadRequest("Not an AI server"))
}
