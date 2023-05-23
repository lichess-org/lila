package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.chat.Chat
import lila.common.HTTPRequest
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
    negotiate(
      html =
        if (!pov.game.started) notFound
        else
          PreventTheft(pov) {
            pov.game.playableByAi ?? env.fishnet.player(pov.game)
            env.tournament.api.gameView.player(pov) flatMap { tour =>
              gameC.preloadUsers(pov.game) zip
                (pov.game.simulId ?? env.simul.repo.find) zip
                getPlayerChat(pov.game, tour.map(_.tour)) zip
                (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip
                (pov.game.isSwitchable ?? otherPovs(pov.game)) zip
                env.bookmark.api.exists(pov.game, ctx.me) zip
                env.api.roundApi.player(pov, tour, lila.api.Mobile.Api.currentVersion) map {
                  case ((((((_, simul), chatOption), crosstable), playing), bookmarked), data) =>
                    simul foreach env.simul.api.onPlayerConnection(pov.game, ctx.me)
                    Ok(
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
                    ).noCache
                }
            }
          },
      api = apiVersion => {
        if (isTheft(pov)) fuccess(theftResponse)
        else
          env.tournament.api.gameView.mobile(pov.game) flatMap { tour =>
            pov.game.playableByAi ?? env.fishnet.player(pov.game)
            gameC.preloadUsers(pov.game) zip
              env.api.roundApi.player(pov, tour, apiVersion) zip
              getPlayerChat(pov.game, none) map { case ((_, data), chat) =>
                Ok(data.add("chat", chat.flatMap(_.game).map(c => lila.chat.JsonView(c.chat)))).noCache
              }
          }
      }
    )

  def player(fullId: GameFullId) = Open:
    env.round.proxyRepo.pov(fullId) flatMap {
      case Some(pov) => renderPlayer(pov)
      case None      => userC.tryRedirect(fullId into UserStr) getOrElse notFound
    }

  private def otherPovs(game: GameModel)(using ctx: Context) =
    ctx.me ?? { user =>
      env.round.proxyRepo urgentGames user map {
        _ filter { pov =>
          pov.gameId != game.id && pov.game.isSwitchable && pov.game.isSimul == game.isSimul
        }
      }
    }

  private def getNext(currentGame: GameModel)(povs: List[Pov]) =
    povs find { pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock)
    }

  def whatsNext(fullId: GameFullId) = Open:
    OptionFuResult(env.round.proxyRepo.pov(fullId)): currentPov =>
      if currentPov.isMyTurn
      then Ok(Json.obj("nope" -> true)).toFuccess
      else
        otherPovs(currentPov.game) map getNext(currentPov.game) map { next =>
          Ok(Json.obj("next" -> next.map(_.fullId)))
        }

  def next(gameId: GameId) = Auth { ctx ?=> me =>
    OptionFuResult(env.round.proxyRepo game gameId) { currentGame =>
      otherPovs(currentGame) map getNext(currentGame) map {
        _ orElse Pov(currentGame, me)
      } flatMap {
        case Some(next) => renderPlayer(next)
        case None =>
          fuccess(Redirect(currentGame.simulId match {
            case Some(simulId) => routes.Simul.show(simulId)
            case None          => routes.Round.watcher(gameId, "white")
          }))
      }
    }
  }

  def watcher(gameId: GameId, color: String) = Open:
    proxyPov(gameId, color) flatMap {
      case Some(pov) =>
        getUserStr("pov").map(_.id).fold(watch(pov)) { requestedPov =>
          (pov.player.userId, pov.opponent.userId) match
            case (Some(_), Some(opponent)) if opponent == requestedPov =>
              Redirect(routes.Round.watcher(gameId, (!pov.color).name)).toFuccess
            case (Some(player), Some(_)) if player == requestedPov =>
              Redirect(routes.Round.watcher(gameId, pov.color.name)).toFuccess
            case _ => Redirect(routes.Round.watcher(gameId, "white")).toFuccess
        }
      case None =>
        userC.tryRedirect(gameId into UserStr) getOrElse
          challengeC.showId(gameId into lila.challenge.Challenge.Id)
    }

  private def proxyPov(gameId: GameId, color: String): Fu[Option[Pov]] =
    chess.Color.fromName(color) ?? {
      env.round.proxyRepo.pov(gameId, _)
    }

  private[controllers] def watch(pov: Pov, userTv: Option[UserModel] = None)(using
      ctx: Context
  ): Fu[Result] =
    playablePovForReq(pov.game) match
      case Some(player) if userTv.isEmpty => renderPlayer(pov withColor player.color)
      case _ if pov.game.variant == chess.variant.RacingKings && pov.color.black =>
        if (userTv.isDefined) watch(!pov, userTv)
        else Redirect(routes.Round.watcher(pov.gameId, "white")).toFuccess
      case _ =>
        negotiate(
          html = {
            if (pov.game.replayable) analyseC.replay(pov, userTv = userTv)
            else if (HTTPRequest.isHuman(ctx.req))
              env.tournament.api.gameView.watcher(pov.game) zip
                (pov.game.simulId ?? env.simul.repo.find) zip
                getWatcherChat(pov.game) zip
                (ctx.noBlind ?? env.game.crosstableApi.withMatchup(pov.game)) zip
                env.bookmark.api.exists(pov.game, ctx.me) flatMap {
                  case ((((tour, simul), chat), crosstable), bookmarked) =>
                    env.api.roundApi.watcher(
                      pov,
                      tour,
                      lila.api.Mobile.Api.currentVersion,
                      tv = userTv.map { u =>
                        lila.round.OnTv.User(u.id)
                      }
                    ) map { data =>
                      Ok(
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
                      )
                    }
                }
            else
              for { // web crawlers don't need the full thing
                initialFen <- env.game.gameRepo.initialFen(pov.gameId)
                pgn        <- env.api.pgnDump(pov.game, initialFen, none, PgnDump.WithFlags(clocks = false))
              } yield Ok(html.round.watcher.crawler(pov, initialFen, pgn))
          },
          api = apiVersion =>
            for {
              tour     <- env.tournament.api.gameView.watcher(pov.game)
              data     <- env.api.roundApi.watcher(pov, tour, apiVersion, tv = none)
              analysis <- env.analyse.analyser get pov.game
              chat     <- getWatcherChat(pov.game)
            } yield Ok {
              data
                .add("chat" -> chat.map(c => lila.chat.JsonView(c.chat)))
                .add("analysis" -> analysis.map(a => lila.analyse.JsonView.mobile(pov.game, a)))
            }
        ) dmap (_.noCache)

  private[controllers] def getWatcherChat(
      game: GameModel
  )(using ctx: Context): Fu[Option[lila.chat.UserChat.Mine]] = {
    ctx.noKid && (ctx.noBot || ctx.userId.exists(game.userIds.contains)) && ctx.me.fold(
      HTTPRequest isHuman ctx.req
    )(env.chat.panic.allowed) && {
      game.finishedOrAborted || !ctx.userId.exists(game.userIds.contains)
    }
  } ?? {
    val id = ChatId(s"${game.id}/w")
    env.chat.api.userChat.findMineIf(id, ctx.me, !game.justCreated) flatMap { chat =>
      env.user.lightUserApi.preloadMany(chat.chat.userIds) inject chat.some
    }
  }

  private[controllers] def getPlayerChat(game: GameModel, tour: Option[Tour])(using
      ctx: Context
  ): Fu[Option[Chat.GameOrEvent]] =
    ctx.noKid ?? {
      def toEventChat(resource: String)(c: lila.chat.UserChat.Mine) =
        Chat
          .GameOrEvent(
            Right(
              (
                c truncate 100,
                lila.chat.Chat.ResourceId(resource)
              )
            )
          )
          .some
      (game.tournamentId, game.simulId, game.swissId) match
        case (Some(tid), _, _) =>
          {
            ctx.isAuth && tour.fold(true)(tournamentC.canHaveChat(_, none))
          } ?? env.chat.api.userChat.cached
            .findMine(ChatId(tid), ctx.me)
            .dmap(toEventChat(s"tournament/$tid"))
        case (_, Some(sid), _) =>
          env.chat.api.userChat.cached.findMine(sid into ChatId, ctx.me).dmap(toEventChat(s"simul/$sid"))
        case (_, _, Some(sid)) =>
          env.swiss.api
            .roundInfo(SwissId(sid))
            .flatMapz(swissC.canHaveChat)
            .flatMapz {
              env.chat.api.userChat.cached
                .findMine(sid into ChatId, ctx.me)
                .dmap(toEventChat(s"swiss/$sid"))
            }
        case _ =>
          game.hasChat ?? {
            env.chat.api.playerChat.findIf(ChatId(game.id), !game.justCreated) map { chat =>
              Chat
                .GameOrEvent(
                  Left(
                    Chat.Restricted(
                      chat,
                      restricted = game.fromLobby && ctx.isAnon
                    )
                  )
                )
                .some
            }
          }
    }

  def sides(gameId: GameId, color: String) = Open:
    OptionFuResult(proxyPov(gameId, color)): pov =>
      env.tournament.api.gameView.withTeamVs(pov.game) zip
        (pov.game.simulId ?? env.simul.repo.find) zip
        env.game.gameRepo.initialFen(pov.game) zip
        env.game.crosstableApi.withMatchup(pov.game) zip
        env.bookmark.api.exists(pov.game, ctx.me) map {
          case ((((tour, simul), initialFen), crosstable), bookmarked) =>
            Ok(html.game.bits.sides(pov, initialFen, tour, crosstable, simul, bookmarked = bookmarked))
        }

  def writeNote(gameId: GameId) = AuthBody { ctx ?=> me =>
    import play.api.data.Forms.*
    import play.api.data.*
    Form(single("text" -> text))
      .bindFromRequest()
      .fold(
        _ => fuccess(BadRequest),
        text => env.round.noteApi.set(gameId, me.id, text.trim take 10000)
      )
  }

  def readNote(gameId: GameId) = Auth { _ ?=> me =>
    env.round.noteApi.get(gameId, me.id) dmap { Ok(_) }
  }

  def continue(id: GameId, mode: String) = Open:
    OptionResult(env.game.gameRepo game id): game =>
      Redirect(
        "%s?fen=%s#%s".format(
          routes.Lobby.home,
          get("fen") | (chess.format.Fen write game.chess).value,
          mode
        )
      )

  def resign(fullId: GameFullId) = Open:
    OptionFuRedirect(env.round.proxyRepo.pov(fullId)): pov =>
      if isTheft(pov) then
        lila.log("round").warn(s"theft resign $fullId ${ctx.ip}")
        fuccess(routes.Lobby.home)
      else
        env.round resign pov
        akka.pattern.after(500.millis, env.system.scheduler)(fuccess(routes.Lobby.home))

  def mini(gameId: GameId, color: String) = Open:
    OptionOk(
      chess.Color.fromName(color).??(env.round.proxyRepo.povIfPresent(gameId, _)) orElse env.game.gameRepo
        .pov(gameId, color)
    )(html.game.mini(_))

  def miniFullId(fullId: GameFullId) = Open:
    OptionOk(env.round.proxyRepo.povIfPresent(fullId) orElse env.game.gameRepo.pov(fullId))(
      html.game.mini(_)
    )

  def apiAddTime(anyId: GameAnyId, seconds: Int) = Scoped(_.Challenge.Write) { _ ?=> me =>
    import lila.round.actorApi.round.Moretime
    if (seconds < 1 || seconds > 86400) BadRequest.toFuccess
    else
      env.round.proxyRepo.game(lila.game.Game anyToId anyId) flatMap {
        _.flatMap { Pov(_, me) }.?? { pov =>
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
    Ok(html.site.helpModal.round).toFuccess
