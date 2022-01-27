package views.html.analyse

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object help {

  private def header(text: Frag) =
    tr(
      th(colspan := 2)(
        p(text)
      )
    )
  private def row(keys: Frag, desc: Frag) =
    tr(
      td(cls := "keys")(keys),
      td(cls := "desc")(desc)
    )
  private val or             = raw("""<or>/</or>""")
  private def k(str: String) = raw(s"""<kbd>$str</kbd>""")

  def apply(isStudy: Boolean)(implicit ctx: Context) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          header(trans.navigateMoveTree()),
          row(frag(k("←"), or, k("→")), trans.keyMoveBackwardOrForward()),
          row(frag(k("j"), or, k("k")), trans.keyMoveBackwardOrForward()),
          row(frag(k("↑"), or, k("↓")), trans.keyGoToStartOrEnd()),
          row(frag(k("0"), or, k("$")), trans.keyGoToStartOrEnd()),
          row(frag(k("shift"), k("←"), or, k("shift"), k("→")), trans.keyEnterOrExitVariation()),
          row(frag(k("shift"), k("J"), or, k("shift"), k("K")), trans.keyEnterOrExitVariation()),
          header(trans.analysisOptions()),
          row(frag(k("shift"), k("I")), trans.inlineNotation()),
          row(frag(k("l")), trans.toggleLocalAnalysis()),
          row(frag(k("z")), trans.toggleAllAnalysis()),
          row(frag(k("a")), trans.bestMoveArrow()),
          row(frag(k("space")), trans.playComputerMove()),
          row(frag(k("x")), trans.showThreat()),
          row(frag(k("e")), trans.openingEndgameExplorer()),
          row(frag(k("f")), trans.flipBoard()),
          row(frag(k("c")), trans.focusChat()),
          row(frag(k("shift"), k("C")), trans.keyShowOrHideComments()),
          row(frag(k("?")), trans.showHelpDialog()),
          isStudy option frag(
            header(trans.study.studyActions()),
            row(frag(k("d")), trans.study.commentThisPosition()),
            row(frag(k("g")), trans.study.annotateWithGlyphs()),
            row(frag(k("n")), trans.study.nextChapter()),
            row(frag(k("p")), trans.study.prevChapter())
          ),
          header(trans.mouseTricks()),
          tr(
            td(cls := "mouse", colspan := 2)(
              ul(
                li(trans.youCanAlsoScrollOverTheBoardToMoveInTheGame()),
                li(trans.scrollOverComputerVariationsToPreviewThem()),
                li(trans.analysisShapesHowTo())
              )
            )
          )
        )
      )
    )
}
