package lila.app
package actor

import akka.actor._

import lila.game.Pov
import views.{ html => V }

final private[app] class Renderer extends Actor {

  def receive = {

    case lila.tv.actorApi.RenderFeaturedJs(game) =>
      sender() ! V.game.bits.featuredJs(Pov first game).render

    case lila.tournament.Tournament.TournamentTable(tours) =>
      sender() ! V.tournament.bits.enterable(tours).render

    case lila.puzzle.DailyPuzzle.Render(puzzle, sfen, lastUsi) =>
      sender() ! V.puzzle.bits.daily(puzzle, sfen, lastUsi).render

    case streams: lila.streamer.LiveStreams.WithTitles =>
      sender() ! V.streamer.bits.liveStreams(streams).render
  }
}
