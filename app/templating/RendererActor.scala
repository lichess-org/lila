package lila.app
package templating

import akka.actor.*
import views.html as V

import lila.game.Pov

final private[app] class Renderer:

  lila.core.hub.renderer.register:

    case lila.tv.RenderFeaturedJs(game, promise) =>
      promise.success(V.game.mini.noCtx(Pov.naturalOrientation(game), tv = true).render)

    case lila.puzzle.DailyPuzzle.Render(puzzle, fen, lastMove, promise) =>
      promise.success(V.puzzle.bits.daily(puzzle, fen, lastMove).render)
