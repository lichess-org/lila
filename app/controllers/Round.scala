package controllers

import lila.app._
import views._
import lila.user.{ Context, UserRepo }
import lila.game.{ Pov, PlayerRef, GameRepo, Game ⇒ GameModel }
import lila.round.{ RoomRepo, WatcherRoomRepo }
import lila.round.actorApi.round._
import lila.socket.actorApi.{ Forward, GetVersion }
import lila.hub.actorApi.map.{ Tell, Await }
import lila.tournament.{ TournamentRepo, Tournament ⇒ Tourney }
import makeTimeout.large

import akka.pattern.ask
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.templates.Html

object Round extends LilaController with TheftPrevention {

  private def env = Env.round
  private def bookmarkApi = Env.bookmark.api
  private def analyser = Env.analyse.analyser

  def websocketWatcher(gameId: String, color: String) = Socket[JsValue] { implicit ctx ⇒
    (get("sri") |@| getInt("version")).tupled ?? {
      case (uid, version) ⇒ env.socketHandler.watcher(gameId, color, version, uid, ctx)
    }
  }

  def websocketPlayer(fullId: String) = Socket[JsValue] { implicit ctx ⇒
    (get("sri") |@| getInt("version") |@| get("tk2")).tupled ?? {
      case (uid, version, token) ⇒ env.socketHandler.player(fullId, version, uid, token, ctx)
    }
  }

  def signedJs(gameId: String) = OpenNoCtx { req ⇒
    JsOk(GameRepo token gameId map Env.game.gameJs.sign, CACHE_CONTROL -> "max-age=3600")
  }

  def player(fullId: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo pov fullId) { pov ⇒
      if (pov.game.playableByAi) env.roundMap ! Tell(pov.game.id, AiPlay(_ ⇒ ()))
      pov.game.started.fold(
        PreventTheft(pov) {
          (pov.game.hasChat optionFu {
            RoomRepo room pov.gameId map { room ⇒
              html.round.roomInner(room.decodedMessages)
            }
          }) zip
            env.version(pov.gameId) zip
            (bookmarkApi userIdsByGame pov.game) zip
            pov.opponent.userId.??(UserRepo.isEngine) zip
            (analyser has pov.gameId) zip
            (pov.game.tournamentId ?? TournamentRepo.byId) map {
              case (((((roomHtml, v), bookmarkers), engine), analysed), tour) ⇒
                Ok(html.round.player(
                  pov,
                  v,
                  engine,
                  roomHtml,
                  bookmarkers,
                  analysed,
                  tour = tour))
            }
        },
        Redirect(routes.Setup.await(fullId)).fuccess
      )
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo.pov(gameId, color)) { pov ⇒
      pov.game.started.fold(watch _, join _)(pov)
    }
  }

  private def join(pov: Pov)(implicit ctx: Context): Fu[Result] =
    GameRepo initialFen pov.gameId zip env.version(pov.gameId) map {
      case (fen, version) ⇒ Ok(html.setup.join(
        pov, version, Env.setup.friendConfigMemo get pov.game.id, fen
      ))
    }

  private def watch(pov: Pov)(implicit ctx: Context): Fu[Result] =
    bookmarkApi userIdsByGame pov.game zip
      env.version(pov.gameId) zip
      (WatcherRoomRepo room pov.gameId map { room ⇒
        html.round.watcherRoomInner(room.decodedMessages)
      }) zip
      (analyser has pov.gameId) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) map {
        case ((((bookmarkers, v), roomHtml), analysed), tour) ⇒
          Ok(html.round.watcher(
            pov, v, roomHtml, bookmarkers, analysed, tour))
      }

  def tableWatcher(gameId: String, color: String) = Open { implicit ctx ⇒
    OptionOk(GameRepo.pov(gameId, color)) { html.round.table.watch(_) }
  }

  def tablePlayer(fullId: String) = Open { implicit ctx ⇒
    OptionFuOk(GameRepo pov fullId) { pov ⇒
      pov.game.tournamentId ?? TournamentRepo.byId map { tour ⇒
        pov.game.playable.fold(
          html.round.table.playing(pov),
          html.round.table.end(pov, tour))
      }
    }
  }

  def players(gameId: String) = Open { implicit ctx ⇒
    import templating.Environment.playerLink
    JsonOptionOk(GameRepo game gameId map2 { (game: GameModel) ⇒
      (game.players collect {
        case player if player.isHuman ⇒ player.color.name -> playerLink(player).body
      } toMap) ++ ctx.me.??(me ⇒ Map("me" -> me.usernameWithElo))
    })
  }
}
