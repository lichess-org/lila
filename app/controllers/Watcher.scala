package controllers

import play.api.mvc._
import play.api.mvc.Results._
import views._

import lila.app._
import lila.game.Pov
import lila.round.WatcherRoomRepo
import lila.tournament.TournamentRepo
import lila.user.Context

private[controllers] trait Watcher {

  private def env = Env.round
  private def bookmarkApi = Env.bookmark.api
  private def analyser = Env.analyse.analyser

  def watch(pov: Pov, tv: Boolean)(implicit ctx: Context): Fu[Result] =
    bookmarkApi userIdsByGame pov.game zip
      env.version(pov.gameId) zip
      (WatcherRoomRepo room pov.gameId map { room ⇒
        html.round.watcherRoomInner(room.decodedMessages)
      }) zip
      (analyser has pov.gameId) zip
      (pov.game.tournamentId ?? TournamentRepo.byId) map {
        case ((((bookmarkers, v), roomHtml), analysed), tour) ⇒
          Ok(html.round.watcher(
            pov, v, roomHtml, bookmarkers, analysed, tour, tv))
      }
}
