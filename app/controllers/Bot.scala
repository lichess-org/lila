package controllers

import play.api.mvc._
import scala.concurrent.duration._

import lila.app._

final class Bot(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private val BotGameStreamConcurrencyLimitPerUser = new lila.memo.ConcurrencyLimit[String](
    name = "Bot game API concurrency per user",
    key = "botGame.concurrency.limit.user",
    ttl = 20 minutes,
    maxConcurrency = 8
  )
  def gameStream(id: String) = Scoped(_.Bot.Play) { implicit req => me =>
    implicit val lang = reqLang
    WithMyBotGame(id, me) { pov =>
      env.game.gameRepo.withInitialFen(pov.game) map { wf =>
        BotGameStreamConcurrencyLimitPerUser(me.id)(
          env.bot.gameStateStream(wf, pov.color)
        )(apiC.sourceToNdJsonOption)
      }
    }
  }

  def move(id: String, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Bot.Play) { _ => me =>
    WithMyBotGame(id, me) { pov =>
      env.bot.player(pov, me, uci, offeringDraw) inject jsonOkResult addEffect { _ =>
        if (me.noBot) env.slack.api.botApiGame(pov.fullId, me)
      } recover {
        case e: Exception => BadRequest(jsonError(e.getMessage))
      }
    }
  }

  def command(cmd: String) = ScopedBody(_.Bot.Play) { implicit req => me =>
    cmd.split('/') match {
      case Array("account", "upgrade") =>
        env.user.repo.isManaged(me.id) flatMap {
          case true => notFoundJson()
          case _ =>
            env.user.repo.setBot(me) >>
              env.pref.api.setBot(me) >>-
              env.user.lightUserApi.invalidate(me.id) inject jsonOkResult recover {
              case e: lila.base.LilaException => BadRequest(jsonError(e.getMessage))
            }
        }
      case Array("game", id, "chat") =>
        WithMyBotGame(id, me) { pov =>
          env.bot.form.chat.bindFromRequest.fold(
            jsonFormErrorDefaultLang,
            res => env.bot.player.chat(pov.gameId, me, res) inject jsonOkResult
          )
        }
      case Array("game", id, "abort") =>
        WithMyBotGame(id, me) { pov =>
          env.bot.player.abort(pov) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(e.getMessage)
          }
        }
      case Array("game", id, "resign") =>
        WithMyBotGame(id, me) { pov =>
          env.bot.player.resign(pov) inject jsonOkResult recover {
            case e: lila.base.LilaException => BadRequest(e.getMessage)
          }
        }
      case _ => notFoundJson("No such command")
    }
  }

  private def WithMyBotGame(anyId: String, me: lila.user.User)(f: lila.game.Pov => Fu[Result]) =
    env.round.proxyRepo.game(lila.game.Game takeGameId anyId) flatMap {
      case None => NotFound(jsonError("No such game")).fuccess
      case Some(game) =>
        lila.game.Pov(game, me) match {
          case None => NotFound(jsonError("Not your game")).fuccess
          case Some(pov) =>
            if (me.isBot) f(pov)
            else if (lila.bot.BotPlayer canPlayWithoutBotTitle pov.game) f(pov)
            else
              BadRequest(
                jsonError(
                  "This game can only be played with a bot account. See https://lichess.org/api#operation/botAccountUpgrade"
                )
              ).fuccess
        }
    }

  def online = Open { implicit ctx =>
    // env.user.botIds().map(_ take 20) flatMap env.user.repo.byIds map { users =>
    env.user.repo.byIds(env.bot.onlineBots.get) map { users =>
      Ok(views.html.user.bots(users))
    }
  }
}
