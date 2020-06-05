package controllers

import play.api.libs.json._
import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.chat.Chat
import lidraughts.common.HTTPRequest
import lidraughts.game.{ Pov, GameRepo, Game => GameModel, PdnDump, PlayerRef }
import lidraughts.tournament.{ TourMiniView, Tournament => Tour }
import lidraughts.user.{ User => UserModel }
import views._

object Round extends LidraughtsController with TheftPrevention {

  private def env = Env.round
  private def analyser = Env.analyse.analyser

  def websocketWatcher(gameId: String, color: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    proxyPov(gameId, color) flatMap {
      _ ?? { pov =>
        getSocketUid("sri") ?? { uid =>
          val userTv = get("userTv") map UserModel.normalize map { userId =>
            val userTvGameId = ~get("gameId")
            lidraughts.round.actorApi.UserTv(
              userId,
              (userTvGameId.isEmpty && pov.game.finishedOrAborted) ?? GameRepo.lastPlayedPlayingId(userId).map(_.isDefined)
            )
          }
          env.socketHandler.watcher(
            pov = pov,
            uid = uid,
            user = ctx.me,
            ip = ctx.ip,
            userTv = userTv,
            version = getSocketVersion,
            apiVersion = apiVersion,
            mobile = getMobile
          ) map some
        }
      }
    }
  }

  def websocketPlayer(fullId: String, apiVersion: Int) = SocketEither[JsValue] { implicit ctx =>
    env.proxy.pov(fullId) flatMap {
      case Some(pov) =>
        if (isTheft(pov)) fuccess(Left(theftResponse))
        else getSocketUid("sri") match {
          case Some(uid) =>
            requestAiMove(pov) >>
              env.socketHandler.player(pov, uid, ctx.me, ctx.ip, getSocketVersion, apiVersion, getMobile) map Right.apply
          case None => fuccess(Left(NotFound))
        }
      case None => fuccess(Left(NotFound))
    }
  }

  private def requestAiMove(pov: Pov) = pov.game.playableByAi ?? Env.draughtsnet.player(pov.game)

  private def renderPlayer(pov: Pov)(implicit ctx: Context): Fu[Result] = negotiate(
    html = if (!pov.game.started) notFound
    else PreventTheft(pov) {
      myTour(pov.game.tournamentId, true) flatMap { tour =>
        (pov.game.simulId ?? Env.simul.repo.find) flatMap { simul =>
          Game.preloadUsers(pov.game) zip
            getPlayerChat(pov.game, tour.map(_.tour), simul) zip
            (ctx.noBlind ?? Env.game.crosstableApi.withMatchup(pov.game)) zip // probably what raises page mean time?
            (pov.game.isSwitchable ?? otherPovs(pov.game)) zip
            Env.bookmark.api.exists(pov.game, ctx.me) zip
            Env.api.roundApi.player(pov, lidraughts.api.Mobile.Api.currentVersion) map {
              case _ ~ chatOption ~ crosstable ~ playing ~ bookmarked ~ data =>
                simul foreach Env.simul.api.onPlayerConnection(pov.game, ctx.me)
                Ok(html.round.player(pov, data,
                  tour = tour,
                  simul = simul,
                  cross = crosstable,
                  playing = playing,
                  chatOption = chatOption,
                  bookmarked = bookmarked))
            }
        }
      }
    }.mon(_.http.response.player.website),
    api = apiVersion => {
      if (isTheft(pov)) fuccess(theftResponse)
      else Game.preloadUsers(pov.game) zip
        Env.api.roundApi.player(pov, apiVersion) zip
        getPlayerChat(pov.game, none, none) map {
          case _ ~ data ~ chat => Ok {
            data.add("chat", chat.flatMap(_.game).map(c => lidraughts.chat.JsonView(c.chat)))
          }
        }
    }.mon(_.http.response.player.mobile)
  ) map NoCache

  def player(fullId: String) = Open { implicit ctx =>
    OptionFuResult(env.proxy.pov(fullId)) { pov =>
      env.checkOutoftime(pov.game)
      renderPlayer(pov)
    }
  }

  private def orInf(i: Option[Int]) = i getOrElse Int.MaxValue

  private def otherPovs(game: GameModel)(implicit ctx: Context) = ctx.me ?? { user =>
    Env.round.proxy urgentGames user map {
      _ filter { pov =>
        pov.gameId != game.id && pov.game.isSwitchable && pov.game.isSimul == game.isSimul
      }
    }
  }

  private def otherPovsSeq(game: GameModel)(implicit ctx: Context) = ctx.me ?? { user =>
    Env.round.proxy urgentGamesSeq user map {
      _ filter { pov =>
        pov.game.id != game.id && pov.game.isSwitchable && pov.game.isSimul && game.isSimul && pov.game.metadata.simulId == game.metadata.simulId
      }
    }
  }

  private def getNext(currentGame: GameModel)(povs: List[Pov])(implicit ctx: Context) =
    povs find { pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock) && (!pov.game.isWithinTimeOut || orInf(pov.remainingSeconds) < 120)
    }

  private def getNextSeq(currentGame: GameModel)(povs: List[Pov])(implicit ctx: Context) = currentGame.metadata.simulPairing.flatMap { index =>
    val validPovs = povs filter { pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock) && (!pov.game.isWithinTimeOut || orInf(pov.remainingSeconds) < 120)
    }
    validPovs.find(_.game.metadata.simulPairing.??(_ > index)).fold(
      validPovs.find(_.game.metadata.simulPairing.??(_ < index))
    )(_.some)
  }

  def whatsNext(fullId: String) = Open { implicit ctx =>
    OptionFuResult(env.proxy.pov(fullId)) { currentPov =>
      if (currentPov.isMyTurn) fuccess {
        Ok(Json.obj("nope" -> true))
      }
      else otherPovs(currentPov.game) map getNext(currentPov.game) map { next =>
        Ok(Json.obj("next" -> next.map(_.fullId)))
      }
    }
  }

  def next(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.proxy game gameId) { currentGame =>
      otherPovs(currentGame) map getNext(currentGame) map {
        _ orElse Pov(currentGame, me)
      } flatMap {
        case Some(next) => renderPlayer(next)
        case None => fuccess(Redirect(currentGame.simulId match {
          case Some(simulId) => routes.Simul.show(simulId)
          case None => routes.Round.watcher(gameId, "white")
        }))
      }
    }
  }

  def nextSequential(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game gameId) { currentGame =>
      otherPovsSeq(currentGame) map getNextSeq(currentGame) map {
        _ orElse Pov(currentGame, me)
      } flatMap {
        case Some(next) => renderPlayer(next)
        case None => fuccess(Redirect(currentGame.simulId match {
          case Some(simulId) => routes.Simul.show(simulId)
          case None => routes.Round.watcher(gameId, "white")
        }))
      }
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx =>
    proxyPov(gameId, color) flatMap {
      case Some(pov) => get("pov") match {
        case Some(requestedPov) => (pov.player.userId, pov.opponent.userId) match {
          case (Some(_), Some(opponent)) if opponent == requestedPov =>
            Redirect(routes.Round.watcher(gameId, (!pov.color).name)).fuccess
          case (Some(player), Some(_)) if player == requestedPov =>
            Redirect(routes.Round.watcher(gameId, pov.color.name)).fuccess
          case _ =>
            Redirect(routes.Round.watcher(gameId, "white")).fuccess
        }
        case None => {
          env.checkOutoftime(pov.game)
          watch(pov)
        }
      }
      case None => Challenge showId gameId
    }
  }

  private def proxyPov(gameId: String, color: String): Fu[Option[Pov]] = draughts.Color(color) ?? {
    env.proxy.pov(gameId, _)
  }

  private[controllers] def watch(pov: Pov, userTv: Option[UserModel] = None, userTvGameId: Option[String] = None)(implicit ctx: Context): Fu[Result] =
    playablePovForReq(pov.game) match {
      case Some(player) if userTv.isEmpty => renderPlayer(pov withColor player.color)
      case _ => Game.preloadUsers(pov.game) >> negotiate(
        html = {
          if (getBool("sudo") && isGranted(_.SuperAdmin)) Redirect(routes.Round.player(pov.fullId)).fuccess
          else if (pov.game.replayable) Analyse.replay(pov, userTv = userTv, userTvGameId = userTvGameId)
          else if (HTTPRequest.isHuman(ctx.req))
            myTour(pov.game.tournamentId, false) zip
              (pov.game.simulId ?? Env.simul.repo.find) zip
              getWatcherChat(pov.game) zip
              (ctx.noBlind ?? Env.game.crosstableApi.withMatchup(pov.game)) zip
              Env.api.roundApi.watcher(
                pov,
                lidraughts.api.Mobile.Api.currentVersion,
                tv = userTv.map { u => lidraughts.round.OnUserTv(u.id, userTvGameId) }
              ) zip
                Env.bookmark.api.exists(pov.game, ctx.me) map {
                  case tour ~ simul ~ chat ~ crosstable ~ data ~ bookmarked =>
                    Ok(html.round.watcher(pov, data, tour, simul, crosstable, userTv = userTv, chatOption = chat, bookmarked = bookmarked))
                }
          else for { // web crawlers don't need the full thing
            initialFen <- GameRepo.initialFen(pov.gameId)
            pdn <- Env.api.pdnDump(pov.game, initialFen, none, PdnDump.WithFlags(clocks = false))
          } yield Ok(html.round.watcher.crawler(pov, initialFen, pdn))
        }.mon(_.http.response.watcher.website),
        api = apiVersion => for {
          data <- Env.api.roundApi.watcher(pov, apiVersion, tv = none)
          analysis <- analyser get pov.game
          chat <- getWatcherChat(pov.game)
        } yield Ok {
          data
            .add("chat" -> chat.map(c => lidraughts.chat.JsonView(c.chat)))
            .add("analysis" -> analysis.map(a => lidraughts.analyse.JsonView.mobile(pov.game, a)))
        }
      ) map NoCache
    }

  private def myTour(tourId: Option[String], withTop: Boolean): Fu[Option[TourMiniView]] =
    tourId ?? { Env.tournament.api.miniView(_, withTop) }

  private[controllers] def getWatcherChat(game: GameModel)(implicit ctx: Context): Fu[Option[lidraughts.chat.UserChat.Mine]] = {
    ctx.noKid && ctx.me.fold(true)(Env.chat.panic.allowed) && {
      game.finishedOrAborted || !ctx.userId.exists(game.userIds.contains)
    }
  } ?? {
    val id = Chat.Id(s"${game.id}/w")
    Env.chat.api.userChat.findMineIf(id, ctx.me, !game.justCreated) flatMap { chat =>
      Env.user.lightUserApi.preloadMany(chat.chat.userIds) inject chat.some
    }
  }

  private[controllers] def getPlayerChat(game: GameModel, tour: Option[Tour], simul: Option[lidraughts.simul.Simul])(implicit ctx: Context): Fu[Option[Chat.GameOrEvent]] = ctx.noKid ?? {
    def toEventChat(resource: String)(c: lidraughts.chat.UserChat.Mine) = Chat.GameOrEvent(Right((
      c truncate 100,
      lidraughts.chat.Chat.ResourceId(resource)
    ))).some
    (game.tournamentId, game.simulId) match {
      case (Some(tid), _) => {
        ctx.isAuth && tour.fold(true)(Tournament.canHaveChat(_, none))
      } ?? Env.chat.api.userChat.cached.findMine(Chat.Id(tid), ctx.me).map(toEventChat(s"tournament/$tid"))
      case (_, Some(sid)) if simul.fold(false)(_.canHaveChat(ctx.me)) => game.simulId.?? { sid =>
        Env.chat.api.userChat.cached.findMine(Chat.Id(sid), ctx.me).map(toEventChat(s"simul/$sid"))
      }
      case _ => game.hasChat ?? {
        Env.chat.api.playerChat.findIf(Chat.Id(game.id), !game.justCreated) map { chat =>
          Chat.GameOrEvent(Left(Chat.Restricted(
            chat,
            restricted = game.fromLobby && ctx.isAnon
          ))).some
        }
      }
    }
  }

  def sides(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(proxyPov(gameId, color)) { pov =>
      (pov.game.tournamentId ?? lidraughts.tournament.TournamentRepo.byId) zip
        (pov.game.simulId ?? Env.simul.repo.find) zip
        GameRepo.initialFen(pov.game) zip
        Env.game.crosstableApi.withMatchup(pov.game) zip
        Env.bookmark.api.exists(pov.game, ctx.me) map {
          case tour ~ simul ~ initialFen ~ crosstable ~ bookmarked =>
            Ok(html.game.bits.sides(pov, initialFen, tour, crosstable, simul, bookmarked = bookmarked))
        }
    }
  }

  def writeNote(gameId: String) = AuthBody { implicit ctx => me =>
    import play.api.data.Forms._
    import play.api.data._
    implicit val req = ctx.body
    Form(single("text" -> text)).bindFromRequest.fold(
      err => fuccess(BadRequest),
      text => Env.round.noteApi.set(gameId, me.id, text.trim take 10000)
    )
  }

  def readNote(gameId: String) = Auth { implicit ctx => me =>
    Env.round.noteApi.get(gameId, me.id) map { text =>
      Ok(text)
    }
  }

  def continue(id: String, mode: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect("%s?fen=%s#%s".format(
        routes.Lobby.home(),
        get("fen") | (draughts.format.Forsyth >> game.draughts),
        mode
      ))
    }
  }

  def resign(fullId: String) = Open { implicit ctx =>
    OptionFuRedirect(env.proxy.pov(fullId)) { pov =>
      if (isTheft(pov)) {
        controllerLogger.warn(s"theft resign $fullId ${HTTPRequest.lastRemoteAddress(ctx.req)}")
        fuccess(routes.Lobby.home)
      } else {
        env resign pov
        import scala.concurrent.duration._
        val scheduler = lidraughts.common.PlayApp.system.scheduler
        akka.pattern.after(500 millis, scheduler)(fuccess(routes.Lobby.home))
      }
    }
  }

  def mini(gameId: String, color: String) = Open { implicit ctx =>
    OptionOk(draughts.Color(color).??(env.proxy.povIfPresent(gameId, _)) orElse GameRepo.pov(gameId, color))(html.game.bits.mini(_))
  }

  def miniFullId(fullId: String) = Open { implicit ctx =>
    OptionOk(env.proxy.povIfPresent(fullId) orElse GameRepo.pov(fullId))(html.game.bits.mini(_))
  }
}
