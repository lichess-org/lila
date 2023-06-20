package lila.app
package templating

import akka.actor.*

import lila.game.Pov
import views.{ html as V }

final private[app] class RendererActor extends Actor:

  def receive =

    case lila.tv.RenderFeaturedJs(game) =>
      sender() ! V.game.mini.noCtx(Pov naturalOrientation game, tv = true).render

    case lila.puzzle.DailyPuzzle.Render(puzzle, fen, lastMove) =>
      sender() ! V.puzzle.bits.daily(puzzle, fen, lastMove).render

    case streams: lila.streamer.LiveStreams.WithTitles =>
      sender() ! V.streamer.bits.liveStreams(streams).render
