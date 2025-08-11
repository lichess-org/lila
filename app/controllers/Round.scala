package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.common.Json.given
import scalalib.data.Preload
import lila.core.id.{ GameAnyId, GameFullId }
import lila.round.RoundGame.*
import lila.tournament.Tournament as Tour
import lila.ui.Snippet

final class Round(
    env: Env,
    gameC: => Game,
    challengeC: => Challenge,
    analyseC: => Analyse,
    tournamentC: => Tournament,
    swissC: => Swiss,
    userC: => User
) extends LilaController(env)
    with lila.web.TheftPrevention:

  import env.user.flairApi.given

  private def renderPlayer(pov: Pov)(using ctx: Context): Fu[Result] =
    pov.game.playableByAi.so(env.fishnet.player(pov.game))
    for
      tour <- env.tournament.api.gameView.player(pov)
      users <- env.user.api.gamePlayers(pov.game.userIdPair, pov.game.perfKey)
      _ = gameC.preloadUsers(users)
      res <- negotiateApi(
        html =
          if !pov.game.started then notFound
          else
            PreventTheft(pov):
              for
                (simul, chatOption, crosstable, playing, bookmarked, data) <-
                  (
                    pov.game.simulId.so(env.simul.repo.find),
                    getPlayerChat(pov.game, tour.map(_.tour)),
                    ctx.noBlind.so(env.game.crosstableApi.withMatchup(pov.game)),
                    pov.game.isSwitchable.so(otherPovs(pov.game)),
                    env.bookmark.api.exists(pov.game, ctx.me),
                    env.api.roundApi.player(pov, Preload(users), tour)
                  ).tupled
                _ = simul.foreach(env.simul.api.onPlayerConnection(pov.game, ctx.me))
                page <- renderPage(
                  views.round.player(
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
              jsChat <- chat.flatMap(_.game).map(_.chat).traverse(lila.chat.JsonView.asyncLines)
            yield Ok(data.add("chat", jsChat)).noCache
      )
    yield res.enforceCrossSiteIsolation

  def player(fullId: GameFullId) = Open:
    env.round.proxyRepo
      .pov(fullId)
      .flatMap:
        case Some(pov) => renderPlayer(pov)
        case None => userC.tryRedirect(fullId.into(UserStr)).getOrElse(notFound)

  private def otherPovs(game: GameModel)(using ctx: Context) =
    ctx.me.so: user =>
      env.round.proxyRepo
        .urgentGames(user)
        .map:
          _.filter: pov =>
            pov.gameId != game.id && pov.game.isSwitchable && pov.game.isSimul == game.isSimul

  private def getNext(currentGame: GameModel)(povs: List[Pov]) =
    povs.find: pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock)

  def whatsNext(fullId: GameFullId) = Open:
    Found(env.round.proxyRepo.pov(fullId)): currentPov =>
      if currentPov.isMyTurn
      then Ok(Json.obj("nope" -> true))
      else
        otherPovs(currentPov.game)
          .map(getNext(currentPov.game))
          .map: next =>
            Ok(Json.obj("next" -> next.map(_.fullId)))

  def next(gameId: GameId) = Auth { ctx ?=> me ?=>
    Found(env.round.proxyRepo.game(gameId)): currentGame =>
      otherPovs(currentGame)
        .map(getNext(currentGame))
        .map(_.orElse(Pov(currentGame, me)))
        .flatMap:
          case Some(next) => renderPlayer(next)
          case None =>
            Redirect(currentGame.simulId match
              case Some(simulId) => routes.Simul.show(simulId)
              case None => routes.Round.watcher(gameId, Color.white))
  }

  def watcher(gameId: GameId, color: Color) = Open:
    env.round.proxyRepo
      .pov(gameId, color)
      .flatMap:
        case Some(pov) =>
          getUserStr("pov")
            .map(_.id)
            .fold(watch(pov)): requestedPov =>
              (pov.player.userId, pov.opponent.userId) match
                case (Some(_), Some(opponent)) if opponent == requestedPov =>
                  Redirect(routes.Round.watcher(gameId, !pov.color))
                case (Some(player), Some(_)) if player == requestedPov =>
                  Redirect(routes.Round.watcher(gameId, pov.color))
                case _ => Redirect(routes.Round.watcher(gameId, Color.white))
        case None =>
          userC
            .tryRedirect(gameId.into(UserStr))
            .getOrElse(challengeC.showId(gameId.into(lila.challenge.ChallengeId)))

  private def isBlockedByPlayer(game: GameModel)(using Context) =
    game.isBeingPlayed.so(env.relation.api.isBlockedByAny(game.userIds))

  private[controllers] def watch(pov: Pov, userTv: Option[UserModel] = None)(using
      ctx: Context
  ): Fu[Result] =
    playablePovForReq(pov.game) match
      case Some(player) if userTv.isEmpty => renderPlayer(pov.withColor(player.color))
      case _ if pov.game.variant == chess.variant.RacingKings && pov.color.black =>
        if userTv.isDefined then watch(!pov, userTv)
        else Redirect(routes.Round.watcher(pov.gameId, Color.white))
      case _ =>
        isBlockedByPlayer(pov.game).flatMap:
          if _ then notFound
          else
            negotiateApi(
              html =
                if pov.game.replayable then analyseC.replay(pov, userTv = userTv)
                else if HTTPRequest.isHuman(ctx.req) then
                  for
                    users <- env.user.api.gamePlayers(pov.game.userIdPair, pov.game.perfKey)
                    tour <- env.tournament.api.gameView.watcher(pov.game)
                    simul <- pov.game.simulId.so(env.simul.repo.find)
                    chat <- getWatcherChat(pov.game)
                    crosstable <- ctx.noBlind.so(env.game.crosstableApi.withMatchup(pov.game))
                    bookmarked <- env.bookmark.api.exists(pov.game, ctx.me)
                    tv = userTv.map: u =>
                      lila.round.OnTv.User(u.id)
                    data <- env.api.roundApi.watcher(pov, users, tour, tv)
                    page <- renderPage:
                      views.round.watcher(
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
                    pgn <- env.api
                      .pgnDump(pov.game, initialFen, none, lila.game.PgnDump.WithFlags(clocks = false))
                    page <- renderPage(views.round.crawler(pov, initialFen, pgn))
                  yield Ok(page)
              ,
              api = _ =>
                for
                  users <- env.user.api.gamePlayers(pov.game.userIdPair, pov.game.perfKey)
                  tour <- env.tournament.api.gameView.watcher(pov.game)
                  data <- env.api.roundApi.watcher(pov, users, tour, tv = none)
                  analysis <- env.analyse.analyser.get(pov.game)
                  chat <- getWatcherChat(pov.game)
                  jsChat <- chat.map(_.chat).traverse(lila.chat.JsonView.asyncLines)
                yield Ok:
                  data
                    .add("chat" -> jsChat)
                    .add("analysis" -> analysis.map(a => lila.analyse.JsonView.mobile(pov.game, a)))
            ).dmap(_.noCache)

  private[controllers] def getWatcherChat(
      game: GameModel
  )(using ctx: Context): Fu[Option[lila.chat.UserChat.Mine]] = {
    (ctx.noBot || ctx.userId.exists(game.userIds.has)) &&
    (ctx.isAuth || HTTPRequest.isHuman(ctx.req)) && {
      game.finishedOrAborted || !ctx.userId.exists(game.userIds.has)
    }
  }.optionFu:
    env.chat.api.userChat.findMine(ChatId(s"${game.id}/w"), !game.justCreated)

  private[controllers] def getPlayerChat(game: GameModel, tour: Option[Tour])(using
      ctx: Context
  ): Fu[Option[Chat.GameOrEvent]] =
    def toEventChat(resource: String)(c: lila.chat.UserChat.Mine) =
      Chat
        .GameOrEvent:
          Right:
            (c.truncate(100), lila.chat.Chat.ResourceId(resource))
        .some
    (game.tournamentId, game.simulId, game.swissId) match
      case (Some(tid), _, _) =>
        val hasChat = ctx.isAuth && tour.forall(tournamentC.canHaveChat(_, none))
        hasChat.so(
          env.chat.api.userChat.cached
            .findMine(tid.into(ChatId))
            .dmap(toEventChat(s"tournament/$tid"))
        )
      case (_, Some(sid), _) =>
        env.chat.api.userChat.cached.findMine(sid.into(ChatId)).dmap(toEventChat(s"simul/$sid"))
      case (_, _, Some(sid)) =>
        env.swiss.api
          .roundInfo(sid)
          .flatMapz(swissC.canHaveChat)
          .flatMapz:
            env.chat.api.userChat.cached
              .findMine(sid.into(ChatId))
              .dmap(toEventChat(s"swiss/$sid"))
      case _ =>
        game.hasChat.so:
          for
            chat <- env.chat.api.playerChat.findIf(game.id.into(ChatId), !game.justCreated)
            lines <- lila.chat.JsonView.asyncLines(chat)
          yield Chat
            .GameOrEvent:
              Left:
                Chat.Restricted(chat, lines, restricted = game.sourceIs(_.Lobby) && ctx.isAnon)
            .some

  def sides(gameId: GameId, color: Color) = Open:
    FoundSnip(env.round.proxyRepo.pov(gameId, color)): pov =>
      (
        env.tournament.api.gameView.withTeamVs(pov.game),
        pov.game.simulId.so(env.simul.repo.find),
        env.game.gameRepo.initialFen(pov.game),
        env.game.crosstableApi.withMatchup(pov.game),
        env.bookmark.api.exists(pov.game, ctx.me)
      ).flatMapN: (tour, simul, initialFen, crosstable, bookmarked) =>
        Snippet(views.game.sides(pov, initialFen, tour, crosstable, simul, bookmarked = bookmarked))

  def writeNote(gameId: GameId) = AuthBody { ctx ?=> me ?=>
    bindForm(env.round.noteApi.form)(
      _ => BadRequest,
      text => env.round.noteApi.set(gameId, me, text.trim.take(10000)).inject(jsonOkResult)
    )
  }

  def readNote(gameId: GameId) = Auth { _ ?=> me ?=>
    env.round.noteApi.get(gameId, me).dmap { Ok(_) }
  }

  def continue(id: GameId, mode: String) = Open:
    Found(env.game.gameRepo.game(id)): game =>
      Redirect:
        "%s?fen=%s#%s".format(
          routes.Lobby.home,
          get("fen") | (chess.format.Fen.write(game.chess)).value,
          mode
        )

  def resign(fullId: GameFullId) = Open:
    Found(env.round.proxyRepo.pov(fullId)): pov =>
      val redirection = fuccess(Redirect(routes.Lobby.home))
      if isTheft(pov) then
        lila.log("round").warn(s"theft resign $fullId ${ctx.ip}")
        redirection
      else
        env.round.resign(pov)
        akka.pattern.after(500.millis, env.system.scheduler)(redirection)

  def mini(gameId: GameId, color: Color) = Open:
    FoundSnip(
      env.round.proxyRepo
        .povIfPresent(gameId, color)
        .orElse(env.game.gameRepo.pov(gameId, color))
    )(pov => Snippet(views.game.mini(pov)))

  def miniFullId(fullId: GameFullId) = Open:
    FoundSnip(env.round.proxyRepo.povIfPresent(fullId).orElse(env.game.gameRepo.pov(fullId))): pov =>
      Snippet(views.game.mini(pov))

  def apiAddTime(anyId: GameAnyId, seconds: Int) = Scoped(_.Challenge.Write) { _ ?=> me ?=>
    import lila.core.round.Moretime
    env.round.proxyRepo
      .pov(anyId.gameId, me)
      .flatMap:
        _.so: pov =>
          env.round.moretimer
            .isAllowedIn(pov.game, Preload.none, force = true)
            .map:
              if _ then
                env.round.roundApi.tell(pov.gameId, Moretime(pov.playerId, seconds.seconds, force = true))
                jsonOkResult
              else BadRequest(jsonError("This game doesn't allow giving time"))
  }

  def help = Open:
    Ok.snip(lila.web.ui.help.round(ctx.kid.no))
