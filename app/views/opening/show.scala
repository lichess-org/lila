package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ OpeningPage, OpeningQuery }
import lila.puzzle.PuzzleOpening
import lila.opening.NamePart

object show {

  import bits._

  def apply(page: OpeningPage, puzzle: Option[PuzzleOpening.FamilyWithCount])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(page),
      title = s"${trans.opening.txt()} • $name",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, page.opening.flatMap(_.uci.split(" ").lastOption), none, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = page.name,
          url = s"$netBaseUrl${queryUrl(page.query)}",
          description = page.opening.??(_.pgn)
        )
        .some,
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "page box box-pad opening")(
        h1(cls := "opening__title")(
          page.query.prev match {
            case Some(prev) => a(href := queryUrl(prev), title := prev.name, dataIcon := "", cls := "text")
            case None       => a(href := routes.Opening.index, dataIcon := "", cls := "text")
          },
          span(cls := "opening__name-parts")(
            page.nameParts.zipWithIndex map { case (NamePart(name, key), i) =>
              frag(
                if (i == 1) br else if (i > 1) ": " else emptyFrag,
                key.fold(span(name))(q => a(href := routes.Opening.query(q))(name))
              )
            }
          )
        ),
        div(cls := "opening__intro")(
          div(
            cls              := "lpv lpv--preload lpv--moves-bottom",
            st.data("pgn")   := page.query.pgnString,
            st.data("title") := page.opening.map(_.name)
          )(lpvPreload),
          div(cls := "opening__intro__side")(
            config(page),
            winRate(page),
            div(cls := "opening__intro__actions")(
              puzzle.map { p =>
                a(cls := "button text", dataIcon := "", href := routes.Puzzle.show(p.family.key.value))(
                  "Train with puzzles"
                )
              },
              a(
                cls      := "button text",
                dataIcon := "",
                href     := s"${routes.UserAnalysis.pgn(page.query.pgn mkString "_")}#explorer"
              )(
                "View in opening explorer"
              )
            ),
            canvas(cls := "opening__popularity__chart")
            // div(cls := "opening__intro__text")("Text here, soon.")
          )
        ),
        whatsNext(page)
      )
    }
}
