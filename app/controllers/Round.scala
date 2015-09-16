package controllers

import akka.pattern.ask
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, PlayerRef, GameRepo, Game => GameModel }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._
import lila.tournament.{ TournamentRepo, Tournament => Tourney, MiniStanding }
import lila.user.{ User => UserModel, UserRepo }
import makeTimeout.large
import views._

object Round extends LilaController with TheftPrevention {

  private def env = Env.round
  private def bookmarkApi = Env.bookmark.api
  private def analyser = Env.analyse.analyser

  def websocketWatcher(gameId: String, color: String) = SocketOption[JsValue] { implicit ctx =>
    (get("sri") |@| getInt("version")).tupled ?? {
      case (uid, version) => env.socketHandler.watcher(
        gameId = gameId,
        colorName = color,
        version = version,
        uid = uid,
        user = ctx.me,
        ip = ctx.ip,
        userTv = get("userTv"))
    }
  }

  def websocketPlayer(fullId: String, apiVersion: Int) = SocketEither[JsValue] { implicit ctx =>
    GameRepo pov fullId flatMap {
      case Some(pov) =>
        if (isTheft(pov)) fuccess(Left(theftResponse))
        else (get("sri") |@| getInt("version")).tupled match {
          case Some((uid, version)) => env.socketHandler.player(
            pov, version, uid, ~get("ran"), ctx.me, ctx.ip
          ) map Right.apply
          case None => fuccess(Left(NotFound))
        }
      case None => fuccess(Left(NotFound))
    }
  }

  private def renderPlayer(pov: Pov)(implicit ctx: Context): Fu[Result] = negotiate(
    html = {
      if (pov.game.playableByAi) env.roundMap ! Tell(pov.game.id, AiPlay)
      pov.game.started.fold(
        PreventTheft(pov) {
          myTour(pov.game.tournamentId, true) zip
            (pov.game.simulId ?? Env.simul.repo.find) zip
            Env.game.crosstableApi(pov.game) zip
            (!pov.game.isTournament ?? otherPovs(pov.gameId)) flatMap {
              case (((tour, simul), crosstable), playing) =>
                simul foreach Env.simul.api.onPlayerConnection(pov.game, ctx.me)
                Env.api.roundApi.player(pov, lila.api.Mobile.Api.currentVersion) map { data =>
                  Ok(html.round.player(pov, data,
                    tour = tour,
                    simul = simul,
                    cross = crosstable,
                    playing = playing,
                    prefs = ctx.isAuth option (Env.pref.forms miniPrefOf ctx.pref)))
                }
            }
        },
        Redirect(routes.Setup.await(pov.fullId)).fuccess
      )
    },
    api = apiVersion => {
      if (isTheft(pov)) fuccess(theftResponse)
      else {
        if (pov.game.playableByAi) env.roundMap ! Tell(pov.game.id, AiPlay)
        Env.api.roundApi.player(pov, apiVersion) map { Ok(_) }
      }
    }
  ) map NoCache

  def player(fullId: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo pov fullId) { pov =>
      env.checkOutoftime(pov.game)
      renderPlayer(pov)
    }
  }

  private def otherPovs(gameId: String)(implicit ctx: Context) = ctx.me ?? { user =>
    GameRepo urgentGames user map {
      _ filter { _.game.id != gameId }
    }
  }

  private def getNext(currentGame: GameModel)(povs: List[Pov])(implicit ctx: Context) =
    povs find { pov =>
      pov.isMyTurn && (pov.game.hasClock || !currentGame.hasClock)
    }

  def others(gameId: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game gameId) { currentGame =>
      otherPovs(gameId) map { povs =>
        Ok(html.round.others(povs))
      }
    }
  }

  def whatsNext(gameId: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game gameId) { currentGame =>
      otherPovs(gameId) map getNext(currentGame) map { next =>
        Ok(Json.obj("next" -> next.map(_.fullId)))
      }
    }
  }

  def next(gameId: String) = Auth { implicit ctx =>
    me =>
      OptionFuResult(GameRepo game gameId) { currentGame =>
        otherPovs(gameId) map getNext(currentGame) map {
          _ orElse Pov(currentGame, me)
        } flatMap {
          case Some(next) => renderPlayer(next)
          case None => fuccess(Redirect(currentGame.simulId match {
            case Some(simulId) => routes.Simul.show(simulId)
            case None          => routes.Round.watcher(gameId, "white")
          }))
        }
      }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { pov =>
      env.checkOutoftime(pov.game)
      watch(pov)
    }
  }

  def watch(pov: Pov, userTv: Option[UserModel] = None)(implicit ctx: Context): Fu[Result] =
    negotiate(
      html =
        if (getBool("sudo") && isGranted(_.SuperAdmin)) Redirect(routes.Round.player(pov.fullId)).fuccess
        else if (pov.game.replayable) Analyse.replay(pov, userTv = userTv)
        else if (pov.game.joinable) join(pov)
        else ctx.userId.flatMap(pov.game.playerByUserId) ifTrue pov.game.playable match {
          case Some(player) => renderPlayer(pov withColor player.color)
          case None if HTTPRequest.isHuman(ctx.req) =>
            myTour(pov.game.tournamentId, false) zip
              (pov.game.simulId ?? Env.simul.repo.find) zip
              Env.game.crosstableApi(pov.game) zip
              Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = none) map {
                case (((tour, simul), crosstable), data) =>
                  Ok(html.round.watcher(pov, data, tour, simul, crosstable, userTv = userTv))
              }
          case _ => // web crawlers don't need the full thing
            GameRepo.initialFen(pov.game.id) zip
              Env.game.crosstableApi(pov.game) map {
                case (initialFen, crosstable) =>
                  val pgn = Env.api.pgnDump(pov.game, initialFen)
                  Ok(html.round.watcherBot(pov, initialFen, pgn, crosstable))
              }
        },
      api = apiVersion => Env.api.roundApi.watcher(pov, apiVersion, tv = none) map { Ok(_) }
    ) map NoCache

  private def myTour(tourId: Option[String], withStanding: Boolean)(implicit ctx: Context): Fu[Option[MiniStanding]] =
    tourId ?? { tid =>
      Env.tournament.api.miniStanding(tid, ctx.userId, withStanding)
    }

  private def join(pov: Pov)(implicit ctx: Context): Fu[Result] =
    GameRepo initialFen pov.game zip
      Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = none) zip
      ((pov.player.userId orElse pov.opponent.userId) ?? UserRepo.byId) map {
        case ((fen, data), opponent) => Ok(html.setup.join(
          pov, data, opponent, Env.setup.friendConfigMemo get pov.game.id, fen))
      }

  def playerText(fullId: String) = Open { implicit ctx =>
    OptionResult(GameRepo pov fullId) { pov =>
      if (ctx.blindMode) Ok(html.game.textualRepresentation(pov, true))
      else BadRequest
    }
  }

  def watcherText(gameId: String, color: String) = Open { implicit ctx =>
    OptionResult(GameRepo.pov(gameId, color)) { pov =>
      if (ctx.blindMode) Ok(html.game.textualRepresentation(pov, false))
      else BadRequest
    }
  }

  def sidesWatcher(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { sides(_, false) }
  }

  def sidesPlayer(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { sides(_, true) }
  }

  def writeNote(gameId: String) = AuthBody { implicit ctx =>
    me =>
      import play.api.data.Forms._
      import play.api.data._
      implicit val req = ctx.body
      Form(single("text" -> text)).bindFromRequest.fold(
        err => fuccess(BadRequest),
        text => Env.round.noteApi.set(gameId, me.id, text.trim take 10000))
  }

  private def sides(pov: Pov, isPlayer: Boolean)(implicit ctx: Context) =
    myTour(pov.game.tournamentId, isPlayer) zip
      (pov.game.simulId ?? Env.simul.repo.find) zip
      GameRepo.initialFen(pov.game) zip
      Env.game.crosstableApi(pov.game) map {
        case (((tour, simul), initialFen), crosstable) =>
          Ok(html.game.sides(pov, initialFen, tour, crosstable, simul))
      }

  def continue(id: String, mode: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect("%s?fen=%s#%s".format(
        routes.Lobby.home(),
        get("fen") | (chess.format.Forsyth >> game.toChess),
        mode))
    }
  }

  def resign(fullId: String) = Open { implicit ctx =>
    OptionResult(GameRepo pov fullId) { pov =>
      env.resign(pov)
      Redirect(routes.Lobby.home)
    }
  }

  def mini(gameId: String, color: String) = Open { implicit ctx =>
    OptionOk(GameRepo.pov(gameId, color)) { pov =>
      html.game.mini(pov)
    }
  }
}
