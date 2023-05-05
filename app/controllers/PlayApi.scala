package controllers

import play.api.mvc.*
import scala.util.chaining.*

import lila.app.{ given, * }
import lila.game.Pov
import lila.user.{ User as UserModel }

// both bot & board APIs
final class PlayApi(env: Env, apiC: => Api)(using akka.stream.Materializer) extends LilaController(env):

  private given (using req: RequestHeader): play.api.i18n.Lang = reqLang

  // bot endpoints

  def botGameStream(id: GameAnyId) = Scoped(_.Bot.Play) { req ?=> me =>
    WithPovAsBot(id, me) { impl.gameStream(me, _) }
  }

  def botMove(id: GameAnyId, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Bot.Play) { _ ?=> me =>
    WithPovAsBot(id, me) { impl.move(me, _, uci, offeringDraw) }
  }

  def botCommand(cmd: String) = ScopedBody(_.Bot.Play) { req ?=> me =>
    if cmd == "account/upgrade" then
      env.user.repo
        .isManaged(me.id)
        .flatMap:
          if _ then notFoundJson()
          else
            env.tournament.api.withdrawAll(me) >>
              env.team.cached.teamIdsList(me.id).flatMap { env.swiss.api.withdrawAll(me, _) } >>
              env.user.repo.setBot(me) >>
              env.pref.api.setBot(me) >>
              env.streamer.api.delete(me) >>-
              env.user.lightUserApi.invalidate(me.id) pipe
              toResult recover { case lila.base.LilaInvalid(msg) =>
                BadRequest(jsonError(msg))
              }
    else impl.command(me, cmd)(WithPovAsBot)
  }

  // board endpoints

  def boardGameStream(id: GameAnyId) = Scoped(_.Board.Play) { req ?=> me =>
    WithPovAsBoard(id, me) { impl.gameStream(me, _) }
  }

  def boardMove(id: GameAnyId, uci: String, offeringDraw: Option[Boolean]) =
    Scoped(_.Board.Play) { _ ?=> me =>
      WithPovAsBoard(id, me):
        impl.move(me, _, uci, offeringDraw)
    }

  def boardCommandPost(cmd: String) = ScopedBody(_.Board.Play) { req ?=> me =>
    impl.command(me, cmd)(WithPovAsBoard)
  }

  // common code for bot & board APIs
  private object impl:

    def gameStream(me: UserModel, pov: Pov)(using RequestHeader) =
      env.game.gameRepo.withInitialFen(pov.game) map { wf =>
        apiC.sourceToNdJsonOption(env.bot.gameStateStream(wf, pov.color, me))
      }

    def move(me: UserModel, pov: Pov, uci: String, offeringDraw: Option[Boolean]) =
      env.bot.player(pov, me, uci, offeringDraw) pipe toResult

    def command(me: UserModel, cmd: String)(
        as: (GameAnyId, UserModel) => (Pov => Fu[Result]) => Fu[Result]
    )(using Request[?]): Fu[Result] =
      cmd.split('/') match
        case Array("game", id, "chat") =>
          as(GameAnyId(id), me): pov =>
            env.bot.form.chat
              .bindFromRequest()
              .fold(
                jsonFormErrorDefaultLang,
                res => env.bot.player.chat(pov.gameId, me, res) inject jsonOkResult
              ) pipe catchClientError
        case Array("game", id, "abort") =>
          as(GameAnyId(id), me): pov =>
            env.bot.player.abort(pov) pipe toResult
        case Array("game", id, "resign") =>
          as(GameAnyId(id), me): pov =>
            env.bot.player.resign(pov) pipe toResult
        case Array("game", id, "draw", bool) =>
          as(GameAnyId(id), me): pov =>
            fuccess(env.bot.player.setDraw(pov, lila.common.Form.trueish(bool))) pipe toResult
        case Array("game", id, "takeback", bool) =>
          as(GameAnyId(id), me): pov =>
            fuccess(env.bot.player.setTakeback(pov, lila.common.Form.trueish(bool))) pipe toResult
        case Array("game", id, "claim-victory") =>
          as(GameAnyId(id), me): pov =>
            env.bot.player.claimVictory(pov) pipe toResult
        case Array("game", id, "berserk") =>
          as(GameAnyId(id), me): pov =>
            fuccess:
              if env.bot.player.berserk(pov.game, me) then jsonOkResult
              else JsonBadRequest(jsonError("Cannot berserk"))
        case _ => notFoundJson("No such command")

  def boardCommandGet(cmd: String) = ScopedBody(_.Board.Play) { _ ?=> me =>
    cmd.split('/') match
      case Array("game", id, "chat") => WithPovAsBoard(GameAnyId(id), me)(getChat)
      case _                         => notFoundJson("No such command")
  }

  def botCommandGet(cmd: String) = ScopedBody(_.Bot.Play) { _ ?=> me =>
    cmd.split('/') match
      case Array("game", id, "chat") => WithPovAsBot(GameAnyId(id), me)(getChat)
      case _                         => notFoundJson("No such command")
  }

  private def getChat(pov: Pov) =
    env.chat.api.userChat.find(ChatId(pov.game.id)) map lila.chat.JsonView.boardApi map JsonOk

  // utils

  private def toResult(f: Funit): Fu[Result] = catchClientError(f inject jsonOkResult)
  private def catchClientError(f: Fu[Result]): Fu[Result] =
    f recover { case e: lila.round.BenignError =>
      BadRequest(jsonError(e.getMessage))
    }

  private def WithPovAsBot(anyId: GameAnyId, me: UserModel)(f: Pov => Fu[Result]) =
    WithPov(anyId, me): pov =>
      if me.noBot then
        BadRequest:
          jsonError:
            "This endpoint can only be used with a Bot account. See https://lichess.org/api#operation/botAccountUpgrade"
        .toFuccess
      else if !lila.game.Game.isBotCompatible(pov.game) then
        BadRequest(jsonError("This game cannot be played with the Bot API.")).toFuccess
      else f(pov)

  private def WithPovAsBoard(anyId: GameAnyId, me: UserModel)(f: Pov => Fu[Result]) =
    WithPov(anyId, me): pov =>
      if me.isBot then notForBotAccounts.toFuccess
      else if !lila.game.Game.isBoardCompatible(pov.game) then
        BadRequest(jsonError("This game cannot be played with the Board API.")).toFuccess
      else f(pov)

  private def WithPov(anyId: GameAnyId, me: UserModel)(f: Pov => Fu[Result]) =
    env.round.proxyRepo.game(lila.game.Game strToId anyId) flatMap {
      case None       => NotFound(jsonError("No such game")).toFuccess
      case Some(game) => Pov(game, me).fold(NotFound(jsonError("Not your game")).toFuccess)(f)
    }

  def botOnline = Open:
    env.user.repo
      .botsByIds(env.bot.onlineApiUsers.get)
      .map: users =>
        Ok(views.html.user.bots(users))

  def botOnlineApi = Anon:
    apiC
      .jsonDownload:
        env.user.repo
          .botsByIdsCursor(env.bot.onlineApiUsers.get)
          .documentSource(getInt("nb", req) | Int.MaxValue)
          .throttle(50, 1 second)
          .map { env.user.jsonView.full(_, withRating = true, withProfile = true) }
      .toFuccess
