package controllers

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._

object Bot extends LilaController {

  def gameStream(id: String) = Scoped(_.Bot.Play) { req => me =>
    WithMyBotGame(id, me) { pov =>
      lila.game.GameRepo.withInitialFen(pov.game) map { wf =>
        Api.jsonOptionStream(Env.bot.gameStateStream(me, wf, pov.color))
      }
    }
  }

  def move(id: String, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Bot.Play) { _ => me =>
    WithMyBotGame(id, me) { pov =>
      Env.bot.player(pov, me, uci, offeringDraw) inject jsonOkResult recover {
        case e: Exception => BadRequest(jsonError(e.getMessage))
      }
    }
  }

  def command(cmd: String) = ScopedBody(_.Bot.Play) { implicit req => me =>
    cmd.split('/') match {
      case Array("account", "upgrade") =>
        lila.user.UserRepo.setBot(me) >>- Env.user.lightUserApi.invalidate(me.id) inject jsonOkResult recover {
          case e: lila.base.LilaException => BadRequest(jsonError(e.getMessage))
        }
      case Array("game", id, "chat") => WithBot(me) {
        Env.bot.form.chat.bindFromRequest.fold(
          jsonFormErrorDefaultLang,
          res => WithMyBotGame(id, me) { pov =>
            Env.bot.player.chat(pov.gameId, me, res) inject jsonOkResult
          }
        )
      }
      case Array("game", id, "abort") => WithBot(me) {
        WithMyBotGame(id, me) { pov =>
          Env.bot.player.abort(pov) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(e.getMessage)
          }
        }
      }
      case Array("game", id, "resign") => WithBot(me) {
        WithMyBotGame(id, me) { pov =>
          Env.bot.player.resign(pov) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(e.getMessage)
          }
        }
      }
      case _ => notFoundJson("No such command")
    }
  }

  private def WithMyBotGame(anyId: String, me: lila.user.User)(f: lila.game.Pov => Fu[Result]) =
    WithBot(me) {
      Env.round.roundProxyGame(lila.game.Game takeGameId anyId) flatMap {
        case None => NotFound(jsonError("No such game")).fuccess
        case Some(game) => lila.game.Pov(game, me) match {
          case None => NotFound(jsonError("Not your game")).fuccess
          case Some(pov) => f(pov)
        }
      }
    }

  private def WithBot(me: lila.user.User)(f: => Fu[Result]) =
    if (!me.isBot) BadRequest(jsonError("This endpoint only works for bot accounts. See https://lichess.org/api#operation/botAccountUpgrade")).fuccess
    else f
}
