package controllers

import lila.app._
import lila.user.Context

import play.api.mvc._

object Ai extends LilaController {

  private def stockfishServer = Env.ai.stockfishServer
  private def isServer = Env.ai.isServer

  def playStockfish = Open { implicit ctx ⇒
    IfServer {
      stockfishServer.play(
        pgn = getOr("pgn", ""),
        initialFen = get("initialFen"),
        level = getIntOr("level", 1)
      ) fold (
          err ⇒ {
            logwarn("[ai] stochfish server play: " + err)
            BadRequest(err.shows)
          },
          Ok(_)
        )
    }
  }

  def analyseStockfish = Open { implicit ctx ⇒
    IfServer {
      stockfishServer.analyse(
        pgn = getOr("pgn", ""),
        initialFen = get("initialFen")
      ) fold (
          err ⇒ {
            logwarn("[ai] stochfish server analyse: " + err)
            InternalServerError(err.shows)
          },
          analyse ⇒ Ok(analyse("fakeid").encodeInfos)
        )
    }
  }

  private def IfServer(result: ⇒ Fu[Result]) =
    isServer.fold(result, BadRequest("Not an AI server").fuccess)
}
