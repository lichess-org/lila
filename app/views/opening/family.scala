package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.LilaOpeningFamily
import lila.puzzle.PuzzleOpening

object family {

  def apply(fam: LilaOpeningFamily, puzzle: Option[PuzzleOpening.FamilyWithCount])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = jsModule("opening"),
      title = s"${trans.opening.txt()} • ${fam.name}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(fam.full.fen.replace(" ", "_"), chess.White.name, fam.full.uci.split(" ").lastOption, none).url}"
          ).some,
          title = fam.name.value,
          url = s"$netBaseUrl${routes.Opening.family(fam.key.value)}",
          description = fam.full.pgn
        )
        .some
    ) {
      main(cls := "page box box-pad")(
        h1(a(href := routes.Opening.index, dataIcon := "", cls := "text"), fam.name.value),
        h2(fam.full.pgn),
        div(
          cls         := "replay replay--autoload",
          data("pgn") := fam.full.pgn
        ),
        puzzle.map { p =>
          a(href := routes.Puzzle.show(p.family.key.value))("Train with ", fam.name.value, " puzzles")
        }
      )
    }
}
