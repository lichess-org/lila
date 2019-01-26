package views.html.analyse

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object help {

  private def header(text: String) = tr(
    th(colspan := 2)(
      p(text)
    )
  )
  private def row(keys: Frag, desc: String) = tr(
    td(cls := "keys")(keys),
    td(cls := "desc")(desc)
  )
  private val or = raw("""<or>/</or>""")
  private def k(str: String) = raw(s"""<kbd>$str</kbd>""")

  def apply(isStudy: Boolean)(implicit ctx: Context) = frag(
    h2(trans.keyboardShortcuts.frag()),
    table(
      tbody(
        header("Navigate the move tree"),
        row(frag(k("←"), or, k("→")), trans.keyMoveBackwardOrForward.txt()),
        row(frag(k("j"), or, k("k")), trans.keyMoveBackwardOrForward.txt()),
        row(frag(k("↑"), or, k("↓")), trans.keyGoToStartOrEnd.txt()),
        row(frag(k("0"), or, k("$")), trans.keyGoToStartOrEnd.txt()),
        row(frag(k("shift"), k("←"), or, k("shift"), k("→")), trans.keyEnterOrExitVariation.txt()),
        row(frag(k("shift"), k("J"), or, k("shift"), k("K")), trans.keyEnterOrExitVariation.txt()),
        header("Analysis options"),
        row(frag(k("shift"), k("I")), trans.inlineNotation.txt()),
        row(frag(k("l")), "Local computer analysis"),
        row(frag(k("a")), "Computer arrows"),
        row(frag(k("space")), "Play computer best move"),
        row(frag(k("x")), "Show threat"),
        row(frag(k("e")), "Opening/endgame explorer"),
        row(frag(k("f")), trans.flipBoard.txt()),
        row(frag(k("/")), "Focus chat"),
        row(frag(k("shift"), k("C")), trans.keyShowOrHideComments.txt()),
        row(frag(k("?")), "Show this help dialog"),
        isStudy option frag(
          header("Study actions"),
          row(frag(k("c")), "Comment this position"),
          row(frag(k("g")), "Annotate with glyphs")
        ),
        header("Mouse tricks"),
        tr(
          td(cls := "mouse", colspan := 2)(
            ul(
              li(trans.youCanAlsoScrollOverTheBoardToMoveInTheGame.frag()),
              li(trans.analysisShapesHowTo.frag())
            )
          )
        )
      )
    )
  )
}
