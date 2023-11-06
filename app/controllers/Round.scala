package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.chat.Chat
import lila.common.{ Preload, HTTPRequest }
import lila.game.{ Game as GameModel, PgnDump, Pov }
import lila.tournament.{ Tournament as Tour }
import lila.user.{ User as UserModel }

final class Round(
    env: Env,
    gameC: => Game,
    challengeC: => Challenge,
    analyseC: => Analyse,
    tournamentC: => Tournament,
    swissC: => Swiss,
    userC: => User
) extends LilaController(env)
    with TheftPrevention:

  private def renderPlayer(pov: Pov)(using ctx: Context): Fu[Result] =
    pov.game.playableByAi so env.fishnet.player(pov.game)
    for
      tour  <- env.tournament.api.gameView.player(pov)
      users <- env.user.api.gamePlayers(pov.game.userIdPair, pov.game.perfType)
      _ = gameC.preloadUsers(users)
      res <- negotiateApi(
        html =
          if !pov.game.started then notFound
          else
            PreventTheft(pov):
              for
                (((((simul, chatOption), crosstable), playing), bookmarked), data) <-
                  (pov.game.simulId so env.simul.repo.find) zip
                    getPlayerChat(pov.game, tour.map(_.tour)) zip
                    (ctx.noBlind so env.game.crosstableApi.withMatchup(pov.game)) zip
                    (pov.game.isSwitchable so otherPovs(pov.game)) zip
                    env.bookmark.api.exists(pov.game, ctx.me) zip
                    env.api.roundApi.player(pov, Preload(users), tour)
                _ = simul foreach env.simul.api.onPlayerConnection(pov.game, ctx.me)
                page <- renderPage(
                  html.round.player(
                    pov,
                    data,
                    tour = tour,
                    simul = simul,
                    cross = crosstable,
                    playing = playing,
                    chatOption = chatOption,
                    bookmarked = bookmarked
                  )
                )
              yield Ok(page).noCache
        ,
        api = _ =>
          if isTheft(pov) then theftResponse
          else
            for
              data <- env.api.roundApi.player(pov, Preload(users), tour)
              chat <- getPlayerChat(pov.game, none)
            yield Ok(data.add("chat", chat.flatMap(_.game).map(c => lila.chat.JsonView(c.chat)))).noCache
      )
    yield res

  def player(fullId: GameFullId) = Open:
    env.round.proxyRepo.pov(fullId) flatMap {
      case Some(pov) => renderPlayer(pov)
      case None      => userC.tryRedirect(fullId into UserStr) getOrElse notFound
    }

  private def otherPovs(game: GameModel)(using ctx: Context) =
    ctx.me.so: user =>
      env.round.proxyRepo urgentGames user map {
        _.filter: pov =>
          pov.gameId != game.id && pov.game.isSwitchable && pov.game.isSimul == game.isSimul
      }

  private def getNext(currentGame: GameModel)(povs: List[Pov]) =
    povs.find: pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock)

  def whatsNext(fullId: GameFullId) = Open:
    Found(env.round.proxyRepo.pov(fullId)): currentPov =>
      if currentPov.isMyTurn
      then Ok(Json.obj("nope" -> true))
      else
        otherPovs(currentPov.game) map getNext(currentPov.game) map { next =>
          Ok(Json.obj("next" -> next.map(_.fullId)))
        }

  def next(gameId: GameId) = Auth { ctx ?=> me ?=>
    Found(env.round.proxyRepo game gameId): currentGame =>
      otherPovs(currentGame) map getNext(currentGame) map {
        _ orElse Pov(currentGame, me)
      } flatMap {
        case Some(next) => renderPlayer(next)
        case None =>
          Redirect(currentGame.simulId match
            case Some(simulId) => routes.Simul.show(simulId)
            case None          => routes.Round.watcher(gameId, "white")
          )
      }
  }

  def watcher(gameId: GameId, color: String) = Open:
    proxyPov(gameId, color) flatMap {
      case Some(pov) =>
        getUserStr("pov")
          .map(_.id)
          .fold(watch(pov)): requestedPov =>
            (pov.player.userId, pov.opponent.userId) match
              case (Some(_), Some(opponent)) if opponent == requestedPov =>
                Redirect(routes.Round.watcher(gameId, (!pov.color).name))
              case (Some(player), Some(_)) if player == requestedPov =>
                Redirect(routes.Round.watcher(gameId, pov.color.name))
              case _ => Redirect(routes.Round.watcher(gameId, "white"))
      case None =>
        userC.tryRedirect(gameId into UserStr) getOrElse
          challengeC.showId(gameId into lila.challenge.Challenge.Id)
    }

  private def proxyPov(gameId: GameId, color: String): Fu[Option[Pov]] =
    chess.Color.fromName(color) so {
      env.round.proxyRepo.pov(gameId, _)
    }

  private[controllers] def watch(pov: Pov, userTv: Option[UserModel] = None)(using
      ctx: Context
  ): Fu[Result] =
    playablePovForReq(pov.game) match
      case Some(player) if userTv.isEmpty => renderPlayer(pov withColor player.color)
      case _ if pov.game.variant == chess.variant.RacingKings && pov.color.black =>
        if userTv.isDefined then watch(!pov, userTv)
        else Redirect(routes.Round.watcher(pov.gameId, "white"))
      case _ =>
        negotiateApi(
          html =
            if pov.game.replayable then analyseC.replay(pov, userTv = userTv)
            else if HTTPRequest.isHuman(ctx.req) then
              for
                users <- env.user.api.gamePlayers(pov.game.userIdPair, pov.game.perfType)
                ((((tour, simul), chat), crosstable), bookmarked) <- env.tournament.api.gameView
                  .watcher(pov.game) zip
                  (pov.game.simulId so env.simul.repo.find) zip
                  getWatcherChat(pov.game) zip
                  (ctx.noBlind so env.game.crosstableApi.withMatchup(pov.game)) zip
                  env.bookmark.api.exists(pov.game, ctx.me)
                tv = userTv.map: u =>
                  lila.round.OnTv.User(u.id)
                data <- env.api.roundApi.watcher(pov, users, tour, tv)
                page <- renderPage:
                  html.round.watcher(
                    pov,
                    data,
                    tour.map(_.tourAndTeamVs),
                    simul,
                    crosstable,
                    userTv = userTv,
                    chatOption = chat,
                    bookmarked = bookmarked
                  )
              yield Ok(page)
            else
              for // web crawlers don't need the full thing
                initialFen <- env.game.gameRepo.initialFen(pov.gameId)
                pgn        <- env.api.pgnDump(pov.game, initialFen, none, PgnDump.WithFlags(clocks = false))
                page       <- renderPage(html.round.watcher.crawler(pov, initialFen, pgn))
              yield Ok(page)
          ,
          api = _ =>
            for
              users    <- env.user.api.gamePlayers(pov.game.userIdPair, pov.game.perfType)
              tour     <- env.tournament.api.gameView.watcher(pov.game)
              data     <- env.api.roundApi.watcher(pov, users, tour, tv = none)
              analysis <- env.analyse.analyser get pov.game
              chat     <- getWatcherChat(pov.game)
            yield Ok:
              data
                .add("chat" -> chat.map(c => lila.chat.JsonView(c.chat)))
                .add("analysis" -> analysis.map(a => lila.analyse.JsonView.mobile(pov.game, a)))
        ) dmap (_.noCache)

  private[controllers] def getWatcherChat(
      game: GameModel
  )(using ctx: Context): Fu[Option[lila.chat.UserChat.Mine]] = {
    ctx.kid.no && (ctx.noBot || ctx.userId.exists(game.userIds.has)) && ctx.me.fold(
      HTTPRequest isHuman ctx.req
    )(env.chat.panic.allowed(_)) && {
      game.finishedOrAborted || !ctx.userId.exists(game.userIds.has)
    }
  }.so:
    val id = ChatId(s"${game.id}/w")
    env.chat.api.userChat.findMineIf(id, !game.justCreated) flatMap { chat =>
      env.user.lightUserApi.preloadMany(chat.chat.userIds) inject chat.some
    }

  private[controllers] def getPlayerChat(game: GameModel, tour: Option[Tour])(using
      ctx: Context
  ): Fu[Option[Chat.GameOrEvent]] =
    ctx.kid.no.so:
      def toEventChat(resource: String)(c: lila.chat.UserChat.Mine) =
        Chat
          .GameOrEvent:
            Right:
              (c truncate 100, lila.chat.Chat.ResourceId(resource))
          .some
      (game.tournamentId, game.simulId, game.swissId) match
        case (Some(tid), _, _) =>
          val hasChat = ctx.isAuth && tour.forall(tournamentC.canHaveChat(_, none))
          hasChat so env.chat.api.userChat.cached
            .findMine(ChatId(tid))
            .dmap(toEventChat(s"tournament/$tid"))
        case (_, Some(sid), _) =>
          env.chat.api.userChat.cached.findMine(sid into ChatId).dmap(toEventChat(s"simul/$sid"))
        case (_, _, Some(sid)) =>
          env.swiss.api
            .roundInfo(SwissId(sid))
            .flatMapz(swissC.canHaveChat)
            .flatMapz:
              env.chat.api.userChat.cached
                .findMine(sid into ChatId)
                .dmap(toEventChat(s"swiss/$sid"))
        case _ =>
          game.hasChat.so:
            env.chat.api.playerChat.findIf(ChatId(game.id), !game.justCreated) map { chat =>
              Chat
                .GameOrEvent:
                  Left:
                    Chat.Restricted(chat, restricted = game.fromLobby && ctx.isAnon)
                .some
            }

  def sides(gameId: GameId, color: String) = Open:
    FoundPage(proxyPov(gameId, color)): pov =>
      env.tournament.api.gameView.withTeamVs(pov.game) zip
        (pov.game.simulId so env.simul.repo.find) zip
        env.game.gameRepo.initialFen(pov.game) zip
        env.game.crosstableApi.withMatchup(pov.game) zip
        env.bookmark.api.exists(pov.game, ctx.me) flatMap {
          case ((((tour, simul), initialFen), crosstable), bookmarked) =>
            html.game.bits.sides(pov, initialFen, tour, crosstable, simul, bookmarked = bookmarked)
        }

  def writeNote(gameId: GameId) = AuthBody { ctx ?=> me ?=>
    import play.api.data.Forms.*
    import play.api.data.*
    Form(single("text" -> text))
      .bindFromRequest()
      .fold(
        _ => BadRequest,
        text => env.round.noteApi.set(gameId, me, text.trim take 10000) inject NoContent
      )
  }

  def readNote(gameId: GameId) = Auth { _ ?=> me ?=>
    env.round.noteApi.get(gameId, me) dmap { Ok(_) }
  }

  def continue(id: GameId, mode: String) = Open:
    Found(env.game.gameRepo game id): game =>
      Redirect:
        "%s?fen=%s#%s".format(
          routes.Lobby.home,
          get("fen") | (chess.format.Fen write game.chess).value,
          mode
        )

  def resign(fullId: GameFullId) = Open:
    Found(env.round.proxyRepo.pov(fullId)): pov =>
      val redirection = fuccess(Redirect(routes.Lobby.home))
      if isTheft(pov) then
        lila.log("round").warn(s"theft resign $fullId ${ctx.ip}")
        redirection
      else
        env.round resign pov
        akka.pattern.after(500.millis, env.system.scheduler)(redirection)

  def mini(gameId: GameId, color: String) = Open:
    FoundPage(
      chess.Color.fromName(color).so(env.round.proxyRepo.povIfPresent(gameId, _)) orElse
        env.game.gameRepo.pov(gameId, color)
    )(html.game.mini(_))

  def miniFullId(fullId: GameFullId) = Open:
    FoundPage(env.round.proxyRepo.povIfPresent(fullId) orElse env.game.gameRepo.pov(fullId))(
      html.game.mini(_)
    )

  def apiAddTime(anyId: GameAnyId, seconds: Int) = Scoped(_.Challenge.Write) { _ ?=> me ?=>
    import lila.round.actorApi.round.Moretime
    env.round.proxyRepo.game(anyId.gameId) flatMap {
      _.flatMap { Pov(_, me) }.so { pov =>
        env.round.moretimer.isAllowedIn(pov.game) map {
          if _ then
            env.round.tellRound(pov.gameId, Moretime(pov.playerId, seconds.seconds))
            jsonOkResult
          else BadRequest(jsonError("This game doesn't allow giving time"))
        }
      }
    }
  }

  def help = Open:
    Ok.page(html.site.help.round)
