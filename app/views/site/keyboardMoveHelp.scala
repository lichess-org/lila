package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object helpModal {

  private def header(text: Frag)          = tr(th(colspan := 2)(p(text)))
  private def row(keys: Frag, desc: Frag) = tr(td(cls := "keys")(keys), td(cls := "desc")(desc))
  private val or                          = tag("or")
  private val kbd                         = tag("kbd")

  def round(implicit ctx: Context) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          header(trans.navigateMoveTree()),
          row(frag(kbd("←"), or, kbd("→")), trans.keyMoveBackwardOrForward()),
          row(frag(kbd("h"), or, kbd("l")), trans.keyMoveBackwardOrForward()),
          row(frag(kbd("↑"), or, kbd("↓")), trans.keyGoToStartOrEnd()),
          row(frag(kbd("k"), or, kbd("j")), trans.keyGoToStartOrEnd()),
          header(trans.other()),
          row(kbd("f"), trans.flipBoard()),
          row(kbd("z"), trans.preferences.zenMode()),
          row(kbd("?"), trans.showHelpDialog())
        )
      )
    )
  def puzzle(implicit ctx: Context) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          header(trans.navigateMoveTree()),
          row(frag(kbd("←"), or, kbd("→")), trans.keyMoveBackwardOrForward()),
          row(frag(kbd("k"), or, kbd("j")), trans.keyMoveBackwardOrForward()),
          row(frag(kbd("↑"), or, kbd("↓")), trans.keyGoToStartOrEnd()),
          row(frag(kbd("0"), or, kbd("$")), trans.keyGoToStartOrEnd()),
          header(trans.analysisOptions()),
          row(kbd("l"), trans.toggleLocalAnalysis()),
          row(kbd("x"), trans.showThreat()),
          row(kbd("space"), trans.playComputerMove()),
          row(kbd("n"), trans.puzzle.nextPuzzle()),
          header(trans.other()),
          row(kbd("f"), trans.flipBoard()),
          row(kbd("z"), trans.preferences.zenMode()),
          row(kbd("?"), trans.showHelpDialog())
        )
      )
    )
  def analyse(isStudy: Boolean)(implicit ctx: Context) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          header(trans.navigateMoveTree()),
          row(frag(kbd("←"), or, kbd("→")), trans.keyMoveBackwardOrForward()),
          row(frag(kbd("k"), or, kbd("j")), trans.keyMoveBackwardOrForward()),
          row(frag(kbd("↑"), or, kbd("↓")), trans.keyGoToStartOrEnd()),
          row(frag(kbd("0"), or, kbd("$")), trans.keyGoToStartOrEnd()),
          row(frag(kbd("shift"), kbd("←"), or, kbd("shift"), kbd("→")), trans.keyEnterOrExitVariation()),
          row(frag(kbd("shift"), kbd("J"), or, kbd("shift"), kbd("K")), trans.keyEnterOrExitVariation()),
          header(trans.analysisOptions()),
          row(frag(kbd("shift"), kbd("I")), trans.inlineNotation()),
          row(kbd("l"), trans.toggleLocalAnalysis()),
          row(kbd("z"), trans.toggleAllAnalysis()),
          row(kbd("a"), trans.bestMoveArrow()),
          row(kbd("space"), trans.playComputerMove()),
          row(kbd("x"), trans.showThreat()),
          row(kbd("e"), trans.openingEndgameExplorer()),
          row(kbd("f"), trans.flipBoard()),
          row(kbd("c"), trans.focusChat()),
          row(frag(kbd("shift"), kbd("C")), trans.keyShowOrHideComments()),
          row(kbd("?"), trans.showHelpDialog()),
          isStudy option frag(
            header(trans.study.studyActions()),
            row(kbd("d"), trans.study.commentThisPosition()),
            row(kbd("g"), trans.study.annotateWithGlyphs()),
            row(kbd("n"), trans.study.nextChapter()),
            row(kbd("p"), trans.study.prevChapter())
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

  def keyboardMove(implicit ctx: Context) = {
    import trans.keyboardMove._
    frag(
      h2(keyboardInputCommands()),
      table(
        tbody(
          header(performAMove()),
          row(kbd("e2e4"), movePieceFromE2ToE4()),
          row(kbd("Nc3"), moveKnightToC3()),
          row(kbd("O-O"), kingsideCastle()),
          row(kbd("O-O-O"), queensideCastle()),
          row(kbd("c8=Q"), promoteC8ToQueen()),
          row(kbd("R@b4"), dropARookAtB4()),
          header(otherCommands()),
          row(kbd("/"), trans.focusChat()),
          row(kbd("clock"), readOutClocks()),
          row(kbd("who"), readOutOpponentName()),
          row(kbd("draw"), offerOrAcceptDraw()),
          row(kbd("resign"), trans.resignTheGame()),
          row(frag(kbd("help"), or, kbd("?")), trans.showHelpDialog()),
          header(tips()),
          tr(
            td(cls := "tips", colspan := 2)(
              ul(
                li(
                  ifTheAboveMoveNotationIsUnfamiliar(),
                  a(target := "_blank", href := "https://en.wikipedia.org/wiki/Algebraic_notation_(chess)")(
                    "Algebraic notation"
                  )
                ),
                li(includingAXToIndicateACapture()),
                li(bothTheLetterOAndTheDigitZero()),
                li(ifItIsLegalToCastleBothWays()),
                li(capitalizationOnlyMattersInAmbiguousSituations()),
                li(toPremoveSimplyTypeTheDesiredPremove())
              )
            )
          )
        )
      )
    )
  }
}
