package controllers

import play.api.mvc._

import lila.app._
import lila.user.Context

object Ai extends LilaController {

  private def stockfishServer = Env.ai.stockfishServer
  private def isServer = Env.ai.isServer

  def playStockfish = Action.async { req ⇒
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

  def analyseStockfish = Action.async { req ⇒
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

  def loadStockfish = Action.async { req ⇒
    IfServer {
      stockfishServer.load map { Ok(_) }
    }
  }

  private def IfServer(result: ⇒ Fu[SimpleResult]) =
    isServer.fold(result, fuccess(BadRequest("Not an AI server")))
}
