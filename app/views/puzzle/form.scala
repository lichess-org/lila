package views.html.puzzle

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def apply(form: Form[_])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = "Submit new puzzles",
      moreCss = cssTag("form3-captcha")
    ) {
      main(cls := "page-small box box-pad report")(
        h1("Submit new puzzles"),
        div(
          frag(
            "If you found an interesting position and want to share the problem with others, you can submit it here.",
            br,
            strong(
              "Puzzle should have only one possible solution. You can chcek this in analysis board with engine set to output multiple pvs."
            ),
            br,
            "Puzzle will be verified by Lishogi together with generating solution and themes. Submitted puzzles are put into a queue and it might take days before they appear publicly.",
            br,
            "Lishogi usually has to go through thousands of positions before a puzzle is found, so the puzzles you submit might be rejected.",
            br,
            "Submit only puzzles you have authored, have the consent of the author or puzzles that are in public domain."
          )
        ),
        postForm(
          cls    := "form3",
          action := s"${routes.Puzzle.addPuzzles}"
        )(
          form3.globalError(form),
          form3.group(
            form("sfens"),
            "Puzzles in SFEN format",
            help = raw("One sfen per line (MAX 5)").some
          )(
            form3.textarea(_)(rows := 5)
          ),
          form3.group(
            form("source"),
            "Source",
            help = raw("Optional - will be shown next to the puzzle").some
          )(form3.input(_)),
          form3.actions(
            a(href := routes.Puzzle.home)(trans.cancel()),
            form3.submit(trans.send())
          )
        )
      )
    }

}
