package lila.web
package ui

import lila.core.i18n.{ I18nKey as trans, Translate }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

object help:

  private def header(text: Frag) = tr(th(colspan := 2)(p(text)))
  private def row(keys: Frag, desc: Frag) = tr(td(cls := "keys")(keys), td(cls := "desc")(desc))
  private val or = tag("or")("/")
  private val kbd = tag("kbd")
  private def voice(text: String) = strong(cls := "val-to-word", text)
  private def phonetic(text: String) = strong(cls := "val-to-word phonetic", text)

  private def navigateMoves(using Translate) = frag(
    header(trans.site.navigateMoveTree()),
    row(frag(kbd("←"), or, kbd("→")), trans.site.keyMoveBackwardOrForward()),
    row(frag(kbd("k"), or, kbd("j")), trans.site.keyMoveBackwardOrForward()),
    row(frag(kbd("↑"), or, kbd("↓")), trans.site.keyGoToStartOrEnd()),
    row(frag(kbd("0"), or, kbd("$")), trans.site.keyGoToStartOrEnd()),
    row(frag(kbd("home"), or, kbd("end")), trans.site.keyGoToStartOrEnd())
  )
  private def flip(using Translate) = row(kbd("f"), trans.site.flipBoard())
  private def zen(using Translate) = row(kbd("z"), trans.preferences.zenMode())
  private def helpDialog(using Translate) = row(kbd("?"), trans.site.showHelpDialog())
  private def localAnalysis(using Translate) = frag(
    row(kbd("l"), trans.site.toggleLocalAnalysis()),
    row(kbd("space"), trans.site.playComputerMove()),
    row(kbd("x"), trans.site.showThreat())
  )
  private def phonetics = "abcdefgh"
    .map(_.toString)
    .map: letter =>
      frag(s"${letter.capitalize} = ", phonetic(letter), ". ")

  def round(hasChat: Boolean)(using Translate) =
    frag(
      h2(trans.site.keyboardShortcuts()),
      table(
        tbody(
          navigateMoves,
          header(trans.site.other()),
          flip,
          zen,
          hasChat.option(
            row(kbd("c"), trans.site.focusChat())
          ),
          helpDialog
        )
      )
    )
  def puzzle(using Translate) =
    frag(
      h2(trans.site.keyboardShortcuts()),
      table(
        tbody(
          navigateMoves,
          header(trans.site.analysisOptions()),
          localAnalysis,
          row(kbd("n"), trans.puzzle.nextPuzzle()),
          header(trans.site.other()),
          flip,
          zen,
          helpDialog
        )
      )
    )
  def analyse(isStudy: Boolean)(using Translate) =
    frag(
      h2(trans.site.keyboardShortcuts()),
      table(
        tbody(
          row(frag(kbd("←"), or, kbd("→")), trans.site.keyMoveBackwardOrForward()),
          row(frag(kbd("k"), or, kbd("j")), trans.site.keyMoveBackwardOrForward()),
          row(kbd("shift"), trans.site.keyCycleSelectedVariation()),
          row(frag(kbd("↑"), or, kbd("↓")), trans.site.cyclePreviousOrNextVariation()),
          row(frag(kbd("shift"), kbd("←"), or, kbd("shift"), kbd("K")), trans.site.keyPreviousBranch()),
          row(frag(kbd("shift"), kbd("→"), or, kbd("shift"), kbd("J")), trans.site.keyNextBranch()),
          row(frag(kbd("↑"), or, kbd("↓")), trans.site.keyGoToStartOrEnd()),
          row(frag(kbd("0"), or, kbd("$")), trans.site.keyGoToStartOrEnd()),
          row(frag(kbd("home"), or, kbd("end")), trans.site.keyGoToStartOrEnd()),
          header(trans.site.analysisOptions()),
          flip,
          row(frag(kbd("shift"), kbd("I")), trans.site.inlineNotation()),
          localAnalysis,
          row(kbd("z"), trans.site.toggleAllAnalysis()),
          row(kbd("a"), trans.site.bestMoveArrow()),
          row(kbd("v"), trans.site.toggleVariationArrows()),
          row(kbd("e"), trans.site.openingEndgameExplorer()),
          row(frag(kbd("shift"), kbd("space")), trans.site.playFirstOpeningEndgameExplorerMove()),
          row(kbd("r"), trans.site.keyRequestComputerAnalysis()),
          row(kbd("enter"), trans.site.keyNextLearnFromYourMistakes()),
          row(kbd("b"), trans.site.keyNextBlunder()),
          row(kbd("m"), trans.site.keyNextMistake()),
          row(kbd("i"), trans.site.keyNextInaccuracy()),
          row(kbd("c"), trans.site.focusChat()),
          row(frag(kbd("shift"), kbd("C")), trans.site.keyShowOrHideComments()),
          helpDialog,
          isStudy.option(
            frag(
              header(trans.study.studyActions()),
              row(kbd("d"), trans.study.commentThisPosition()),
              row(kbd("g"), trans.study.annotateWithGlyphs()),
              row(kbd("n"), trans.study.nextChapter()),
              row(kbd("p"), trans.study.prevChapter()),
              row(frag((1 to 6).map(kbd(_))), trans.site.toggleGlyphAnnotations()),
              row(frag(kbd("shift"), (1 to 8).map(kbd(_))), trans.site.togglePositionAnnotations()),
              row(frag(kbd("ctrl"), kbd("z")), "Undo arrow changes")
            )
          ),
          header(trans.site.mouseTricks()),
          tr(
            td(cls := "mouse", colspan := 2)(
              ul(
                li(trans.site.youCanAlsoScrollOverTheBoardToMoveInTheGame()),
                li(trans.site.scrollOverComputerVariationsToPreviewThem()),
                li(trans.site.analysisShapesHowTo())
              )
            )
          )
        )
      )
    )

  def keyboardMove(using Translate) =
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
          row(kbd("/"), trans.site.focusChat()),
          row(kbd("clock"), readOutClocks()),
          row(kbd("who"), readOutOpponentName()),
          row(kbd("draw"), offerOrAcceptDraw()),
          row(kbd("resign"), trans.site.resignTheGame()),
          row(kbd("zerk"), trans.arena.berserk()),
          row(kbd("next"), trans.puzzle.nextPuzzle()),
          row(kbd("upv"), trans.puzzle.upVote()),
          row(kbd("downv"), trans.puzzle.downVote()),
          row(frag(kbd("help"), or, kbd("?")), trans.site.showHelpDialog()),
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

  def voiceMove(using Translate) =
    import trans.voiceCommands.*
    frag(
      h2(voiceCommands()),
      table(
        tbody(
          tr(th(p(trans.site.instructions()))),
          tr(
            td(cls := "tips")(
              ul(
                li(
                  a(target := "_blank", href := "https://youtu.be/Ibfk4TyDZpY")(
                    watchTheVideoTutorial()
                  )
                ),
                li(
                  instructions1(
                    i(dataIcon := Icon.Voice),
                    i(dataIcon := Icon.InfoCircle),
                    i(dataIcon := Icon.Gear)
                  )
                ),
                li(instructions2()),
                li(instructions3(voice("yes"), voice("no"))),
                li(
                  instructions4(strong("Push to Talk")),
                  strong(" Shift"),
                  " also cancels any ongoing speech."
                ),
                li(
                  "Enable ",
                  a(href := "/account/preferences/game-behavior#moveConfirmation")("Move Confirmation"),
                  " in Settings, set timer off, and set clarity to clear if you are playing blindfolded",
                  " with speech synthesis. This enables spoken move confirmation."
                ),
                li(instructions5(), phonetics),
                li(
                  instructions6(
                    a(target := "_blank", href := "/@/schlawg/blog/how-to-lichess-voice/nWrypoWI")(
                      thisBlogPost()
                    )
                  )
                )
              )
            )
          )
        ),
        div(cls := "commands")(
          table(
            tbody(
              header(trans.keyboardMove.performAMove()),
              row(voice("e,4"), moveToE4OrSelectE4Piece()),
              row(voice("B"), selectOrCaptureABishop()),
              row(voice("N,c,3"), trans.keyboardMove.moveKnightToC3()),
              row(voice("Q,x,R"), takeRookWithQueen()),
              row(voice("c,8,=,Q"), trans.keyboardMove.promoteC8ToQueen()),
              row(voice("castle"), castle()),
              row(voice("O-O-O"), trans.keyboardMove.queensideCastle()),
              row(phonetic("a,7,g,1"), phoneticAlphabetIsBest()),
              row(voice("draw"), trans.keyboardMove.offerOrAcceptDraw()),
              row(voice("resign"), trans.site.resignTheGame()),
              row(voice("takeback"), trans.site.proposeATakeback())
            )
          ),
          table(
            tbody(
              header(trans.keyboardMove.otherCommands()),
              row(voice("no"), cancelTimerOrDenyARequest()),
              row(voice("yes"), playPreferredMoveOrConfirmSomething()),
              row(voice("vocabulary"), "List all available commands"),
              row(voice("blindfold"), "Toggle blindfold mode"),
              row(voice("clock"), "Read out clocks"),
              row(voice("pieces"), "Read out pieces"),
              row(voice("white-pieces"), "Read out white pieces"),
              row(voice("next"), trans.puzzle.nextPuzzle()),
              row(voice("upvote"), trans.puzzle.upVote()),
              row(voice("solve"), showPuzzleSolution()),
              row(voice("help"), trans.site.showHelpDialog()),
              tr(
                td,
                td(button(cls := "button", cls := "all-phrases-button")(trans.site.showMeEverything()))
              )
            )
          )
        )
      )
    )
