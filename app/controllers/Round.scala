package controllers

import akka.pattern.ask
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.game.{ Pov, PlayerRef, GameRepo, Game => GameModel }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._
import lila.tournament.{ TournamentRepo, Tournament => Tourney }
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

  private lazy val theftResponse = Unauthorized(Json.obj(
    "error" -> "This game requires authentication"
  )) as JSON

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
          (pov.game.tournamentId ?? TournamentRepo.byId) zip
            (pov.game.simulId ?? Env.simul.repo.find) zip
            Env.game.crosstableApi(pov.game) zip
            (!pov.game.isTournament ?? otherPovs(pov.gameId)) flatMap {
              case (((tour, simul), crosstable), playing) =>
                simul foreach Env.simul.api.onPlayerConnection(pov.game, ctx.me)
                Env.api.roundApi.player(pov, lila.api.Mobile.Api.currentVersion) map { data =>
                  Ok(html.round.player(pov, data, tour = tour, simul = simul, cross = crosstable, playing = playing))
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
  )

  def player(fullId: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo pov fullId) { pov =>
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
      watch(pov)
    }
  }

  def watch(pov: Pov, userTv: Option[UserModel] = None)(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = if (pov.game.replayable) Analyse.replay(pov, userTv = userTv)
      else if (pov.game.joinable) join(pov)
      else ctx.userId.flatMap(pov.game.playerByUserId) ifTrue pov.game.playable match {
        case Some(player) => renderPlayer(pov withColor player.color)
        case None =>
          (pov.game.tournamentId ?? TournamentRepo.byId) zip
            (pov.game.simulId ?? Env.simul.repo.find) zip
            Env.game.crosstableApi(pov.game) zip
            Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = none) map {
              case (((tour, simul), crosstable), data) =>
                Ok(html.round.watcher(pov, data, tour, simul, crosstable, userTv = userTv))
            }
      },
      api = apiVersion => Env.api.roundApi.watcher(pov, apiVersion, tv = none) map { Ok(_) }
    )

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

  def sideWatcher(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { side(_, false) }
  }

  def sidePlayer(fullId: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo pov fullId) { side(_, true) }
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

  private def side(pov: Pov, isPlayer: Boolean)(implicit ctx: Context) =
    (pov.game.tournamentId ?? TournamentRepo.byId) zip
      (pov.game.simulId ?? Env.simul.repo.find) zip
      GameRepo.initialFen(pov.game) map {
        case ((tour, simul), initialFen) =>
          Ok(html.game.side(pov, initialFen, tour, withTourStanding = isPlayer, simul))
      }

  def continue(id: String, mode: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect("%s?fen=%s#%s".format(
        routes.Lobby.home(),
        get("fen") | (chess.format.Forsyth >> game.toChess),
        mode))
    }
  }
}
