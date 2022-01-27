package controllers

import play.api.mvc._
import scala.concurrent.duration._
import scala.util.chaining._

import lila.app._
import lila.game.Pov
import lila.user.{ User => UserModel }

// both bot & board APIs
final class PlayApi(
    env: Env,
    apiC: => Api
)(implicit
    mat: akka.stream.Materializer
) extends LilaController(env) {

  implicit private def autoReqLang(implicit req: RequestHeader) = reqLang(req)

  // bot endpoints

  def botGameStream(id: String) =
    Scoped(_.Bot.Play) { implicit req => me =>
      WithPovAsBot(id, me) { impl.gameStream(me, _) }
    }

  def botMove(id: String, uci: String, offeringDraw: Option[Boolean]) =
    Scoped(_.Bot.Play) { _ => me =>
      WithPovAsBot(id, me) { impl.move(me, _, uci, offeringDraw) }
    }

  def botCommand(cmd: String) =
    ScopedBody(_.Bot.Play) { implicit req => me =>
      cmd.split('/') match {
        case Array("account", "upgrade") =>
          env.user.repo.isManaged(me.id) flatMap {
            case true => notFoundJson()
            case _ =>
              env.tournament.api.withdrawAll(me) >>
                env.team.cached.teamIdsList(me.id).flatMap { env.swiss.api.withdrawAll(me, _) } >>
                env.user.repo.setBot(me) >>
                env.pref.api.setBot(me) >>
                env.streamer.api.delete(me) >>-
                env.user.lightUserApi.invalidate(me.id) pipe
                toResult recover { case lila.base.LilaInvalid(msg) =>
                  BadRequest(jsonError(msg))
                }
          }
        case _ => impl.command(me, cmd)(WithPovAsBot)
      }
    }

  // board endpoints

  def boardGameStream(id: String) =
    Scoped(_.Board.Play) { implicit req => me =>
      WithPovAsBoard(id, me) { impl.gameStream(me, _) }
    }

  def boardMove(id: String, uci: String, offeringDraw: Option[Boolean]) =
    Scoped(_.Board.Play) { _ => me =>
      WithPovAsBoard(id, me) {
        impl.move(me, _, uci, offeringDraw)
      }
    }

  def boardCommandPost(cmd: String) =
    ScopedBody(_.Board.Play) { implicit req => me =>
      impl.command(me, cmd)(WithPovAsBoard)
    }

  // common code for bot & board APIs
  private object impl {

    def gameStream(me: UserModel, pov: Pov)(implicit req: RequestHeader) =
      env.game.gameRepo.withInitialFen(pov.game) map { wf =>
        apiC.sourceToNdJsonOption(env.bot.gameStateStream(wf, pov.color, me))
      }

    def move(me: UserModel, pov: Pov, uci: String, offeringDraw: Option[Boolean]) =
      env.bot.player(pov, me, uci, offeringDraw) pipe toResult

    def command(me: UserModel, cmd: String)(
        as: (String, UserModel) => (Pov => Fu[Result]) => Fu[Result]
    )(implicit req: Request[_]): Fu[Result] =
      cmd.split('/') match {
        case Array("game", id, "chat") =>
          as(id, me) { pov =>
            env.bot.form.chat
              .bindFromRequest()
              .fold(
                jsonFormErrorDefaultLang,
                res => env.bot.player.chat(pov.gameId, me, res) inject jsonOkResult
              ) pipe catchClientError
          }
        case Array("game", id, "abort") =>
          as(id, me) { pov =>
            env.bot.player.abort(pov) pipe toResult
          }
        case Array("game", id, "resign") =>
          as(id, me) { pov =>
            env.bot.player.resign(pov) pipe toResult
          }
        case Array("game", id, "draw", bool) =>
          as(id, me) { pov =>
            fuccess(env.bot.player.setDraw(pov, lila.common.Form.trueish(bool))) pipe toResult
          }
        case Array("game", id, "takeback", bool) =>
          as(id, me) { pov =>
            fuccess(env.bot.player.setTakeback(pov, lila.common.Form.trueish(bool))) pipe toResult
          }
        case Array("game", id, "claim-victory") =>
          as(id, me) { pov =>
            env.bot.player.claimVictory(pov) pipe toResult
          }
        case _ => notFoundJson("No such command")
      }
  }

  def boardCommandGet(cmd: String) =
    ScopedBody(_.Board.Play) { implicit req => me =>
      cmd.split('/') match {
        case Array("game", id, "chat") => WithPovAsBoard(id, me)(getChat)
        case _                         => notFoundJson("No such command")
      }
    }

  def botCommandGet(cmd: String) =
    ScopedBody(_.Bot.Play) { implicit req => me =>
      cmd.split('/') match {
        case Array("game", id, "chat") => WithPovAsBot(id, me)(getChat)
        case _                         => notFoundJson("No such command")
      }
    }

  private def getChat(pov: Pov) =
    env.chat.api.userChat.find(lila.chat.Chat.Id(pov.game.id)) map lila.chat.JsonView.boardApi map JsonOk

  // utils

  private def toResult(f: Funit): Fu[Result] = catchClientError(f inject jsonOkResult)
  private def catchClientError(f: Fu[Result]): Fu[Result] =
    f recover { case e: lila.round.BenignError =>
      BadRequest(jsonError(e.getMessage))
    }

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

  def botOnline =
    Open { implicit ctx =>
      env.user.repo.botsByIds(env.bot.onlineApiUsers.get) map { users =>
        Ok(views.html.user.bots(users))
      }
    }

  def botOnlineApi =
    Action { implicit req =>
      apiC.jsonStream {
        env.user.repo
          .botsByIdsCursor(env.bot.onlineApiUsers.get)
          .documentSource(getInt("nb", req) | Int.MaxValue)
          .throttle(50, 1 second)
          .map { env.user.jsonView.full(_, withOnline = false, withRating = true) }
      }
    }
}
