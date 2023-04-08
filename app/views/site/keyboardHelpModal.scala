package views.html.site

import play.api.i18n.Lang

import lila.app.templating.Environment.given
import lila.app.ui.ScalatagsTemplate.*

object keyboardHelpModal:

  private def header(text: Frag)          = tr(th(colspan := 2)(p(text)))
  private def row(keys: Frag, desc: Frag) = tr(td(cls := "keys")(keys), td(cls := "desc")(desc))
  private val or                          = tag("or")
  private val kbd                         = tag("kbd")

  private def navigateMoves(using Lang) = frag(
    header(trans.navigateMoveTree()),
    row(frag(kbd("←"), or, kbd("→")), trans.keyMoveBackwardOrForward()),
    row(frag(kbd("k"), or, kbd("j")), trans.keyMoveBackwardOrForward()),
    row(frag(kbd("↑"), or, kbd("↓")), trans.keyGoToStartOrEnd()),
    row(frag(kbd("0"), or, kbd("$")), trans.keyGoToStartOrEnd()),
    row(frag(kbd("home"), or, kbd("end")), trans.keyGoToStartOrEnd())
  )
  private def flip(using Lang)       = row(kbd("f"), trans.flipBoard())
  private def zen(using Lang)        = row(kbd("z"), trans.preferences.zenMode())
  private def helpDialog(using Lang) = row(kbd("?"), trans.showHelpDialog())
  private def localAnalysis(using Lang) = frag(
    row(kbd("l"), trans.toggleLocalAnalysis()),
    row(kbd("space"), trans.playComputerMove()),
    row(kbd("x"), trans.showThreat())
  )

  def round(using Lang) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          navigateMoves,
          header(trans.other()),
          flip,
          zen,
          helpDialog
        )
      )
    )
  def puzzle(using Lang) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          navigateMoves,
          header(trans.analysisOptions()),
          localAnalysis,
          row(kbd("n"), trans.puzzle.nextPuzzle()),
          header(trans.other()),
          flip,
          zen,
          helpDialog
        )
      )
    )
  def analyse(isStudy: Boolean)(using Lang) =
    frag(
      h2(trans.keyboardShortcuts()),
      table(
        tbody(
          navigateMoves,
          row(frag(kbd("shift"), kbd("←"), or, kbd("shift"), kbd("→")), trans.keyEnterOrExitVariation()),
          row(frag(kbd("shift"), kbd("J"), or, kbd("shift"), kbd("K")), trans.keyEnterOrExitVariation()),
          header(trans.analysisOptions()),
          flip,
          row(frag(kbd("shift"), kbd("I")), trans.inlineNotation()),
          localAnalysis,
          row(kbd("z"), trans.toggleAllAnalysis()),
          row(kbd("a"), trans.bestMoveArrow()),
          row(kbd("e"), trans.openingEndgameExplorer()),
          row(kbd("c"), trans.focusChat()),
          row(frag(kbd("shift"), kbd("C")), trans.keyShowOrHideComments()),
          helpDialog,
          isStudy option frag(
            header(trans.study.studyActions()),
            row(kbd("d"), trans.study.commentThisPosition()),
            row(kbd("g"), trans.study.annotateWithGlyphs()),
            row(kbd("n"), trans.study.nextChapter()),
            row(kbd("p"), trans.study.prevChapter()),
            row(frag((1 to 6).map(kbd(_))), "Toggle glyph annotations")
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

  def keyboardMove(using Lang) =
    import trans.keyboardMove.*
    frag(
      h2(keyboardInputCommands()),
      table(
        tbody(
          header(performAMove()),
          row(kbd("e2e4"), movePieceFromE2ToE4()),
          row(kbd("5254"), movePieceFromE2ToE4()),
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
          row(kbd("next"), trans.puzzle.nextPuzzle()),
          row(kbd("upv"), trans.puzzle.upVote()),
          row(kbd("downv"), trans.puzzle.downVote()),
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
