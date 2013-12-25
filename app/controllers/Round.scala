package controllers

import akka.pattern.ask
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.api.templates.Html

import lila.app._
import lila.game.{ Pov, PlayerRef, GameRepo, Game ⇒ GameModel }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round._
import lila.round.{ RoomRepo, WatcherRoomRepo }
import lila.tournament.{ TournamentRepo, Tournament ⇒ Tourney }
import lila.user.{ Context, UserRepo }
import makeTimeout.large
import views._

object Round extends LilaController with TheftPrevention {

  private def env = Env.round
  private def bookmarkApi = Env.bookmark.api
  private def analyser = Env.analyse.analyser

  def websocketWatcher(gameId: String, color: String) = Socket[JsValue] { implicit ctx ⇒
    (get("sri") |@| getInt("version")).tupled ?? {
      case (uid, version) ⇒ env.socketHandler.watcher(gameId, color, version, uid, ctx.me, ctx.ip)
    }
  }

  def websocketPlayer(fullId: String) = Socket[JsValue] { implicit ctx ⇒
    (get("sri") |@| getInt("version")).tupled ?? {
      case (uid, version) ⇒ env.socketHandler.player(fullId, version, uid, ~get("ran"), ctx.me, ctx.ip)
    }
  }

  def signedJs(gameId: String) = OpenNoCtx { req ⇒
    JsOk(fuccess(Env.game.gameJs.sign(env.hijack tokenOf gameId)), CACHE_CONTROL -> "max-age=3600")
  }

  def player(fullId: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo pov fullId) { pov ⇒
      if (pov.game.playableByAi) env.roundMap ! Tell(pov.game.id, AiPlay)
      pov.game.started.fold(
        PreventTheft(pov) {
          (pov.game.hasChat optionFu {
            RoomRepo room pov.gameId map { room ⇒ html.round.roomInner(room.decodedMessages) }
          }) zip
            env.version(pov.gameId) zip
            (bookmarkApi userIdsByGame pov.game) zip
            pov.opponent.userId.??(UserRepo.isEngine) zip
            (analyser has pov.gameId) zip
            (pov.game.tournamentId ?? TournamentRepo.byId) map {
              case (((((roomHtml, v), bookmarkers), engine), analysed), tour) ⇒
                Ok(html.round.player(pov, v, engine, roomHtml, bookmarkers, analysed, tour = tour))
            }
        },
        Redirect(routes.Setup.await(fullId)).fuccess
      )
    }
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx ⇒
    OptionFuResult(GameRepo.pov(gameId, color)) { pov ⇒
      pov.game.joinable.fold(join _, watch _)(pov)
    }
  }

  private def watch(pov: Pov)(implicit ctx: Context): Fu[SimpleResult] =
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

  private def join(pov: Pov)(implicit ctx: Context): Fu[SimpleResult] =
    GameRepo initialFen pov.gameId zip
      env.version(pov.gameId) zip
      ((pov.player.userId orElse pov.opponent.userId) ?? UserRepo.byId) map {
        case ((fen, version), opponent) ⇒ Ok(html.setup.join(
          pov, opponent, version, Env.setup.friendConfigMemo get pov.game.id, fen))
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

  def endWatcher(gameId: String, color: String) = Open { implicit ctx ⇒
    JsonOptionFuOk(GameRepo.pov(gameId, color)) { end(_, false) }
  }

  def endPlayer(fullId: String) = Open { implicit ctx ⇒
    JsonOptionFuOk(GameRepo pov fullId) { end(_, true) }
  }

  private def end(pov: Pov, player: Boolean)(implicit ctx: Context) = {
    import templating.Environment.playerLink
    pov.game.tournamentId ?? TournamentRepo.byId map { tour ⇒
      val players = (pov.game.players collect {
        case player if player.isHuman ⇒ player.color.name -> playerLink(player).body
      } toMap) ++ ctx.me.??(me ⇒ Map("me" -> me.usernameWithRating))
      val table = if (player) html.round.table.end(pov, tour) else html.round.table.watch(pov)
      Json.obj(
        "players" -> players,
        "infobox" -> html.game.infoBox(pov, tour).toString,
        "table" -> table.toString)
    }
  }

  def continue(id: String, mode: String) = Open { implicit ctx ⇒
    OptionResult(GameRepo game id) { game ⇒
      Redirect("%s?fen=%s#%s".format(
        routes.Lobby.home(),
        get("fen") | (chess.format.Forsyth >> game.toChess),
        mode))
    }
  }
}
