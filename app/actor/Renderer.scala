package lidraughts.app
package actor

import akka.actor._
import play.twirl.api.Html

import lidraughts.game.Pov
import views.{ html => V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lidraughts.tv.actorApi.RenderFeaturedJs(game) =>
      sender ! V.game.bits.featuredJs(Pov first game).render

    case lidraughts.tournament.actorApi.TournamentTable(tours) =>
      sender ! V.tournament.enterable(tours).render

    case lidraughts.simul.actorApi.SimulTable(simuls) =>
      sender ! V.simul.bits.allCreated(simuls).render

    case lidraughts.puzzle.RenderDaily(puzzle, fen, lastMove) =>
      sender ! V.puzzle.bits.daily(puzzle, fen, lastMove).render

    case streams: lidraughts.streamer.LiveStreams.WithTitles =>
      sender ! V.streamer.bits.liveStreams(streams).render
  }
}
