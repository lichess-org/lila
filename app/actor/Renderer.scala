package lidraughts.app
package actor

import akka.actor._
import play.twirl.api.Html

import lidraughts.game.Pov
import views.{ html => V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lidraughts.tv.actorApi.RenderFeaturedJs(game) =>
      sender ! V.game.featuredJs(Pov first game)

    case lidraughts.tournament.actorApi.TournamentTable(tours) =>
      sender ! spaceless(V.tournament.enterable(tours))

    case lidraughts.simul.actorApi.SimulTable(simuls) =>
      sender ! spaceless(V.simul.allCreated(simuls))

    case lidraughts.puzzle.RenderDaily(puzzle, fen, lastMove) =>
      sender ! spaceless(V.puzzle.daily(puzzle, fen, lastMove))

    case streams: lidraughts.streamer.LiveStreams.WithTitles => sender ! V.streamer.liveStreams(streams)
  }

  private val spaceRegex = """\s{2,}""".r
  private def spaceless(html: Html) = Html {
    spaceRegex.replaceAllIn(html.body.replace("\\n", " "), " ")
  }
}
