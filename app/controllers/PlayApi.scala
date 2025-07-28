package controllers

import play.api.i18n.Lang
import play.api.mvc.*

import lila.app.{ *, given }
import lila.core.id.GameAnyId

// both bot & board APIs
final class PlayApi(env: Env) extends LilaController(env):

  // bot endpoints

  def botGameStream(id: GameId) = Scoped(_.Bot.Play) { ctx ?=> me ?=>
    WithPovAsBot(id)(impl.gameStream)
  }

  def botMove(id: GameId, uci: String, offeringDraw: Option[Boolean]) = Scoped(_.Bot.Play) { _ ?=> me ?=>
    WithPovAsBot(id) { impl.move(_, uci, offeringDraw) }
  }

  def botCommand(cmd: String) = ScopedBody(_.Bot.Play) { ctx ?=> me ?=>
    if cmd == "account/upgrade" then
      env.user.repo
        .isManaged(me)
        .flatMap:
          if _ then notFoundJson("Managed accounts cannot be bots.")
          else
            {
              for
                _ <- env.tournament.api.withdrawAll(me)
                teamIds <- env.team.cached.teamIdsList(me)
                _ <- env.swiss.api.withdrawAll(me, teamIds)
                _ <- env.user.api.setBot(me)
                _ <- env.pref.api.setBot(me)
                _ <- env.streamer.api.delete(me)
              yield env.user.lightUserApi.invalidate(me)
            }.pipe(toResult).recover { case lila.core.lilaism.LilaInvalid(msg) =>
              BadRequest(jsonError(msg))
            }
    else impl.command(cmd)(WithPovAsBot)
  }

  // board endpoints

  def boardGameStream(id: GameId) = Scoped(_.Board.Play) { ctx ?=> me ?=>
    WithPovAsBoard(id)(impl.gameStream)
  }

  def boardMove(id: GameId, uci: String, offeringDraw: Option[Boolean]) =
    Scoped(_.Board.Play) { _ ?=> me ?=>
      WithPovAsBoard(id): pov =>
        env.bot.boardReport.move(pov.game)
        impl.move(pov, uci, offeringDraw)
    }

  def boardCommandPost(cmd: String) = ScopedBody(_.Board.Play) { ctx ?=> me ?=>
    impl.command(cmd)(WithPovAsBoard)
  }

  // common code for bot & board APIs
  private object impl:

    def gameStream(pov: Pov)(using Context, Me) =
      env.game.gameRepo
        .withInitialFen(pov.game)
        .map: wf =>
          jsOptToNdJson(env.bot.gameStateStream(wf, pov.color))

    def move(pov: Pov, uci: String, offeringDraw: Option[Boolean])(using Me) =
      env.bot.player(pov, uci, offeringDraw).pipe(toResult)

    def command(cmd: String)(
        as: Me ?=> GameId => (Pov => Fu[Result]) => Fu[Result]
    )(using Request[?], Lang)(using me: Me): Fu[Result] =
      cmd.split('/') match
        case Array("game", id, "chat") =>
          as(GameAnyId(id).gameId): pov =>
            bindForm[lila.bot.BotForm.ChatData, Fu[Result]](env.bot.form.chat)(
              doubleJsonFormError,
              res => env.bot.player.chat(pov.gameId, res).inject(jsonOkResult)
            ).pipe(catchClientError)
        case Array("game", id, "abort") =>
          as(GameAnyId(id).gameId): pov =>
            env.bot.player.abort(pov).pipe(toResult)
        case Array("game", id, "resign") =>
          as(GameAnyId(id).gameId): pov =>
            env.bot.player.resign(pov).pipe(toResult)
        case Array("game", id, "draw", bool) =>
          as(GameAnyId(id).gameId): pov =>
            fuccess(env.bot.player.setDraw(pov, lila.common.Form.trueish(bool))).pipe(toResult)
        case Array("game", id, "takeback", bool) =>
          as(GameAnyId(id).gameId): pov =>
            fuccess(env.bot.player.setTakeback(pov, lila.common.Form.trueish(bool))).pipe(toResult)
        case Array("game", id, "claim-victory") =>
          as(GameAnyId(id).gameId): pov =>
            env.bot.player.claimVictory(pov).pipe(toResult)
        case Array("game", id, "claim-draw") =>
          as(GameAnyId(id).gameId): pov =>
            env.bot.player.claimDraw(pov).pipe(toResult)
        case Array("game", id, "berserk") =>
          as(GameAnyId(id).gameId): pov =>
            if !me.isBot && env.bot.player.berserk(pov.game) then jsonOkResult
            else JsonBadRequest(jsonError("Cannot berserk"))
        case _ => notFoundJson("No such command")

  def boardCommandGet(cmd: String) = ScopedBody(_.Board.Play) { _ ?=> me ?=>
    cmd.split('/') match
      case Array("game", id, "chat") => WithPovAsBoard(GameAnyId(id).gameId)(getChat)
      case _ => notFoundJson("No such command")
  }

  def botCommandGet(cmd: String) = ScopedBody(_.Bot.Play) { _ ?=> me ?=>
    cmd.split('/') match
      case Array("game", id, "chat") => WithPovAsBot(GameAnyId(id).gameId)(getChat)
      case _ => notFoundJson("No such command")
  }

  private def getChat(pov: Pov) =
    env.chat.api.userChat.find(pov.game.id.into(ChatId)).map(lila.chat.JsonView.boardApi).map(JsonOk)

  // utils

  private def toResult(f: Funit): Fu[Result] = catchClientError(f.inject(jsonOkResult))
  private def catchClientError(f: Fu[Result]): Fu[Result] =
    f.recover { case e: lila.core.round.BenignError =>
      BadRequest(jsonError(e.getMessage))
    }

  private def WithPovAsBot(id: GameId)(f: Pov => Fu[Result])(using me: Me) =
    WithPov(id): pov =>
      if me.noBot then
        BadRequest:
          jsonError:
            "This endpoint can only be used with a Bot account. See https://lichess.org/api#operation/botAccountUpgrade"
      else
        isReallyBotCompatible(pov.game).flatMap:
          if _ then f(pov)
          else BadRequest(jsonError("This game cannot be played with the Bot API."))

  private def isReallyBotCompatible(game: lila.core.game.Game): Fu[Boolean] =
    lila.game.Game.isBotCompatible(game) match
      case Some(known) => fuccess(known)
      case None => game.tournamentId.so(env.tournament.api.isForBots)

  private def WithPovAsBoard(id: GameId)(f: Pov => Fu[Result])(using ctx: Context)(using Me) =
    WithPov(id): pov =>
      NoBot:
        if !lila.game.Game.isBoardCompatible(pov.game) then
          BadRequest(jsonError("This game cannot be played with the Board API."))
        else f(pov)

  private def WithPov(id: GameId)(f: Pov => Fu[Result])(using me: Me) =
    env.round.proxyRepo
      .game(id)
      .flatMap:
        case None => NotFound(jsonError("No such game"))
        case Some(game) => Pov(game, me).fold(NotFound(jsonError("Not your game")).toFuccess)(f)

  def botOnline = Open:
    for
      users <- env.bot.onlineApiUsers.getUsers
      page <- renderPage(views.user.list.bots(users))
    yield Ok(page)

  def botOnlineApi = Anon:
    for lines <- env.bot.onlineApiUsers.getNdJson(getInt("nb"))
    yield Ok(lines).as(ndJson.contentType)
