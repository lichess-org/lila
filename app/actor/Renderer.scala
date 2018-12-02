package lila.app
package actor

import akka.actor._
import play.twirl.api.Html

import lila.game.Pov
import views.{ html => V }

private[app] final class Renderer extends Actor {

  def receive = {

    case lila.tv.actorApi.RenderFeaturedJs(game) =>
      sender ! V.game.bits.featuredJs(Pov first game)

    case lila.tournament.actorApi.TournamentTable(tours) =>
      sender ! spaceless(V.tournament.enterable(tours))

    case lila.simul.actorApi.SimulTable(simuls) =>
      sender ! spaceless(V.simul.allCreated(simuls))

    case lila.puzzle.RenderDaily(puzzle, fen, lastMove) =>
      sender ! spaceless(V.puzzle.daily(puzzle, fen, lastMove))

    case streams: lila.streamer.LiveStreams.WithTitles => sender ! V.streamer.liveStreams(streams)
  }

  private val spaceRegex = """\s{2,}+""".r
  private def spaceless(html: Html) = Html {
    spaceRegex.replaceAllIn(html.body.replace("\\n", " "), " ")
  }
}
