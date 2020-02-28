package controllers

import play.api.mvc._
import scala.concurrent.duration._
import play.api.i18n.Lang

import lila.app._
import lila.game.Pov
import lila.user.{ User => UserModel }

// both bot & board APIs
final class PlayApi(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  implicit private def autoReqLang(implicit req: RequestHeader) = reqLang(req)

  private val BotGameStreamConcurrencyLimitPerUser = new lila.memo.ConcurrencyLimit[String](
    name = "Bot game API concurrency per user",
    key = "botGame.concurrency.limit.user",
    ttl = 20 minutes,
    maxConcurrency = 8
  )

  // bot endpoints

  def botGameStream(id: String) = Scoped(_.Bot.Play) { implicit req => me =>
    WithPovAsBot(id, me) { impl.gameStream(me, _) }
  }

  def botMove(id: String, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Bot.Play) { _ => me =>
    WithPovAsBot(id, me) { impl.move(me, _, uci, offeringDraw) }
  }

  def botCommand(cmd: String) = ScopedBody(_.Bot.Play) { implicit req => me =>
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
      case _ => impl.command(me, cmd)(WithPovAsBot)
    }
  }

  // board endpoints

  def boardGameStream(id: String) = Scoped(_.Board.Play) { implicit req => me =>
    WithPovAsBoard(id, me) { impl.gameStream(me, _) }
  }

  def boardMove(id: String, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Board.Play) { _ => me =>
    WithPovAsBoard(id, me) { pov =>
      env.slack.api.boardApiMove(pov.fullId, me)
      impl.move(me, pov, uci, offeringDraw)
    }
  }

  def boardCommand(cmd: String) = ScopedBody(_.Board.Play) { implicit req => me =>
    impl.command(me, cmd)(WithPovAsBoard)
  }

  // common code for bot & board APIs
  private object impl {

    def gameStream(me: UserModel, pov: Pov)(implicit lang: Lang) =
      env.game.gameRepo.withInitialFen(pov.game) map { wf =>
        BotGameStreamConcurrencyLimitPerUser(me.id)(
          env.bot.gameStateStream(wf, pov.color, me.isBot)
        )(apiC.sourceToNdJsonOption)
      }

    def move(me: UserModel, pov: Pov, uci: String, offeringDraw: Option[Boolean]) =
      env.bot.player(pov, me, uci, offeringDraw) inject jsonOkResult recover {
        case e: Exception => BadRequest(jsonError(e.getMessage))
      }

    def command(me: UserModel, cmd: String)(
        as: (String, UserModel) => (Pov => Fu[Result]) => Fu[Result]
    )(implicit req: Request[_]): Fu[Result] =
      cmd.split('/') match {
        case Array("game", id, "chat") =>
          as(id, me) { pov =>
            env.bot.form.chat.bindFromRequest.fold(
              jsonFormErrorDefaultLang,
              res => env.bot.player.chat(pov.gameId, me, res) inject jsonOkResult
            )
          }
        case Array("game", id, "abort") =>
          as(id, me) { pov =>
            env.bot.player.abort(pov) inject jsonOkResult recover {
              case e: lila.base.LilaException => BadRequest(e.getMessage)
            }
          }
        case Array("game", id, "resign") =>
          as(id, me) { pov =>
            env.bot.player.resign(pov) inject jsonOkResult recover {
              case e: lila.base.LilaException => BadRequest(e.getMessage)
            }
          }
        case _ => notFoundJson("No such command")
      }
  }

  // utils

  private def WithPovAsBot(anyId: String, me: lila.user.User)(f: Pov => Fu[Result]) =
    WithPov(anyId, me) { pov =>
      if (me.noBot)
        BadRequest(
          jsonError(
            "This endpoint can only be used with a Bot account. See https://lichess.org/api#operation/botAccountUpgrade"
          )
        ).fuccess
      else if (!lila.game.Game.isBotCompatible(pov.game))
        BadRequest(jsonError("This game cannot be played with the Bot API.")).fuccess
      else f(pov)
    }

  private def WithPovAsBoard(anyId: String, me: lila.user.User)(f: Pov => Fu[Result]) =
    WithPov(anyId, me) { pov =>
      if (me.isBot) notForBotAccounts.fuccess
      else if (!lila.game.Game.isBoardCompatible(pov.game))
        BadRequest(jsonError("This game cannot be played with the Board API.")).fuccess
      else f(pov)
    }

  private def WithPov(anyId: String, me: lila.user.User)(f: Pov => Fu[Result]) =
    env.round.proxyRepo.game(lila.game.Game takeGameId anyId) flatMap {
      case None => NotFound(jsonError("No such game")).fuccess
      case Some(game) =>
        Pov(game, me) match {
          case None      => NotFound(jsonError("Not your game")).fuccess
          case Some(pov) => f(pov)
        }
    }

  def botOnline = Open { implicit ctx =>
    env.user.repo.byIds(env.bot.onlineBots.get) map { users =>
      Ok(views.html.user.bots(users))
    }
  }
}
