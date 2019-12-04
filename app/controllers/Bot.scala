package controllers

import play.api.mvc._

import lila.app._

final class Bot(
    env: Env,
    apiC: Api
) extends LilaController(env) {

  def gameStream(id: String) = Scoped(_.Bot.Play) { req => me =>
    WithMyBotGame(id, me) { pov =>
      env.game.gameRepo.withInitialFen(pov.game) map { wf =>
        apiC.jsonOptionStream(env.bot.gameStateStream(wf, pov.color))
      }
    }
  }

  def move(id: String, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Bot.Play) { _ => me =>
    WithMyBotGame(id, me) { pov =>
      env.bot.player(pov, me, uci, offeringDraw) inject jsonOkResult recover {
        case e: Exception => BadRequest(jsonError(e.getMessage))
      }
    }
  }

  def command(cmd: String) = ScopedBody(_.Bot.Play) { implicit req => me =>
    cmd.split('/') match {
      case Array("account", "upgrade") =>
        env.user.userRepo.setBot(me) >>
          env.pref.api.setBot(me) >>-
          env.user.lightUserApi.invalidate(me.id) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(jsonError(e.getMessage))
          }
      case Array("game", id, "chat") => WithBot(me) {
        env.bot.form.chat.bindFromRequest.fold(
          jsonFormErrorDefaultLang,
          res => WithMyBotGame(id, me) { pov =>
            env.bot.player.chat(pov.gameId, me, res) inject jsonOkResult
          }
        )
      }
      case Array("game", id, "abort") => WithBot(me) {
        WithMyBotGame(id, me) { pov =>
          env.bot.player.abort(pov) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(e.getMessage)
          }
        }
      }
      case Array("game", id, "resign") => WithBot(me) {
        WithMyBotGame(id, me) { pov =>
          env.bot.player.resign(pov) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(e.getMessage)
          }
        }
      }
      case _ => notFoundJson("No such command")
    }
  }

  private def WithMyBotGame(anyId: String, me: lila.user.User)(f: lila.game.Pov => Fu[Result]) =
    WithBot(me) {
      env.round.proxyRepo.game(lila.game.Game takeGameId anyId) flatMap {
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
