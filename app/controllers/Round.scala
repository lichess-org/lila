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

  def websocketWatcher(gameId: String, color: String) = Socket[JsValue] { implicit ctx =>
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

  def websocketPlayer(fullId: String, apiVersion: Int) = Socket[JsValue] { implicit ctx =>
    GameRepo pov fullId flatMap {
      _ ?? { pov =>
        (get("sri") |@| getInt("version")).tupled ?? {
          case (uid, version) => env.socketHandler.player(pov, version, uid, ~get("ran"), ctx.me, ctx.ip)
        }
      }
    }
  }

  def signedJs(gameId: String) = OpenNoCtx { req =>
    JsOk(fuccess(Env.game.gameJs.sign(env.hijack tokenOf gameId)), CACHE_CONTROL -> "max-age=3600")
  }

  def player(fullId: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo pov fullId) { pov =>
      if (pov.game.playableByAi) env.roundMap ! Tell(pov.game.id, AiPlay)
      negotiate(
        html = pov.game.started.fold(
          PreventTheft(pov) {
            (pov.game.tournamentId ?? TournamentRepo.byId) zip
              Env.game.crosstableApi(pov.game) flatMap {
                case (tour, crosstable) =>
                  Env.api.roundApi.player(pov, Env.api.version) map { data =>
                    Ok(html.round.player(pov, data, tour = tour, cross = crosstable))
                  }
              }
          },
          Redirect(routes.Setup.await(fullId)).fuccess
        ),
        api = apiVersion => Env.api.roundApi.player(pov, apiVersion) map { Ok(_) }
      )
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { pov =>
      watch(pov)
    }
  }

  def watch(pov: Pov, userTv: Option[UserModel] = None)(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = if (pov.game.replayable) Analyse replay pov
      else if (pov.game.joinable) join(pov)
      else ctx.userId.flatMap(pov.game.playerByUserId).ifTrue(pov.game.playable) match {
        case Some(player) => fuccess(Redirect(routes.Round.player(pov.game fullIdOf player.color)))
        case None =>
          (pov.game.tournamentId ?? TournamentRepo.byId) zip
            Env.game.crosstableApi(pov.game) zip
            Env.api.roundApi.watcher(pov, Env.api.version, tv = false, userTv = userTv) map {
              case ((tour, crosstable), data) =>
                Ok(html.round.watcher(pov, data, tour, crosstable, userTv = userTv))
            }
      },
      api = apiVersion => Env.api.roundApi.watcher(pov, apiVersion, tv = false) map { Ok(_) }
    )

  private def join(pov: Pov)(implicit ctx: Context): Fu[Result] =
    GameRepo initialFen pov.gameId zip
      Env.api.roundApi.player(pov, Env.api.version) zip
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

  private def side(pov: Pov, isPlayer: Boolean)(implicit ctx: Context) =
    pov.game.tournamentId ?? TournamentRepo.byId map { tour =>
      Ok(html.game.side(pov, tour, withTourStanding = isPlayer))
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
