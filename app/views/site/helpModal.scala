package views.html.site

import play.api.i18n.Lang

import lila.app.templating.Environment.given
import lila.app.ui.ScalatagsTemplate.*

object helpModal:

  private def header(text: Frag)          = tr(th(colspan := 2)(p(text)))
  private def row(keys: Frag, desc: Frag) = tr(td(cls := "keys")(keys), td(cls := "desc")(desc))
  private val or                          = tag("or")
  private val kbd                         = tag("kbd")
  private def voice(text: String)         = strong(cls := "val-to-word", text)
  private def phonetic(text: String)      = strong(cls := "val-to-word phonetic", text)

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
  def voiceMove(using Lang) =
    import trans.keyboardMove.*
    frag(
      h2("Voice commands"),
      table(
        tbody(
          tr(th(p("Instructions"))),
          tr(
            td(cls := "tips")(
              ul(
                li(
                  "Use the ",
                  i(dataIcon := ""),
                  " button to toggle voice recognition mode."
                ),
                li(
                  "Your voice audio never leaves your device. Moves are sent as plain text just like those made by mouse or touch."
                ),
                li(
                  "You may speak UCI, SAN, piece names, board squares, or phrases like ",
                  voice("P,x,R"),
                  " and ",
                  voice("x"),
                  ". Click ",
                  strong("Show me everything"),
                  " for a full list."
                ),
                li(
                  "We show colored or numbered arrows for up to 8 available moves when we're not sure. " +
                    "If an arrow shows a growing pie, that move will be played when the pie becomes a full circle."
                ),
                li(
                  "During this countdown, you may only say ",
                  voice("yes"),
                  " to play the move immediately, ",
                  voice("no"),
                  " to cancel, ",
                  voice("stop"),
                  " to stop the clock, or the color/number of an arrow. No other command will be recognized."
                ),
                li(
                  "Higher clarity values will decrease arrows & countdowns but increase the chance of misplays."
                ),
                li(
                  "Clarity, countdown, and arrow display settings are in the voice bar hamburger menu."
                ),
                li(
                  "The phonetic alphabet is ",
                  phonetic("a,b,c,d,e,f,g,h")
                )
              )
            )
          )
        )
      ),
      div(cls := "commands")(
        table(
          tbody(
            header(performAMove()),
            row(frag(voice("e,4"), br, phonetic("e,4")), "Move to e4 or select a piece there"),
            row(voice("N"), "Move my knight or capture a knight"),
            row(frag(voice("B,h,6"), br, phonetic("B,h,6")), "Move bishop to h6"),
            row(voice("Q,x,R"), "Take rook with queen"),
            row(
              frag(voice("c,8,=,N"), br, phonetic("c,8,N")),
              "Move c8 promote to knight"
            ),
            row(voice("castle"), "castle (either side)"),
            row(voice("O-O-O"), "Queenside castle"),
            row(frag(voice("a,7,g,1"), br, phonetic("a,7,g,1")), "Full UCI works too"),
            row(voice("draw"), offerOrAcceptDraw())
          )
        ),
        table(
          tbody(
            header(otherCommands()),
            row(voice("resign"), trans.resignTheGame()),
            row(voice("takeback"), "Request a takeback"),
            row(voice("no"), "Cancel timer or deny a request"),
            row(voice("yes"), "Play preferred move or confirm something"),
            row(voice("stop"), "Stop the timer but keep the arrows"),
            row(voice("mic-off"), "Turn off your microphone"),
            row(voice("next"), trans.puzzle.nextPuzzle()),
            row(voice("upvote"), trans.puzzle.upVote()),
            row(voice("solve"), "Show puzzle solution"),
            row(voice("help"), trans.showHelpDialog()),
            tr(
              td,
              td(button(cls := "button", id := "all-phrases-button")("Show me everything"))
            )
          )
        )
      )
    )
