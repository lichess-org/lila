package views.html.site

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object helpModal:

  private def header(text: Frag)          = tr(th(colspan := 2)(p(text)))
  private def row(keys: Frag, desc: Frag) = tr(td(cls := "keys")(keys), td(cls := "desc")(desc))
  private val or                          = tag("or")
  private val kbd                         = tag("kbd")
  private def voice(text: String)         = tag("voice")(s"\"$text\"")

  private def navigateMoves(implicit lang: Lang) = frag(
    header(trans.navigateMoveTree()),
    row(frag(kbd("←"), or, kbd("→")), trans.keyMoveBackwardOrForward()),
    row(frag(kbd("k"), or, kbd("j")), trans.keyMoveBackwardOrForward()),
    row(frag(kbd("↑"), or, kbd("↓")), trans.keyGoToStartOrEnd()),
    row(frag(kbd("0"), or, kbd("$")), trans.keyGoToStartOrEnd()),
    row(frag(kbd("home"), or, kbd("end")), trans.keyGoToStartOrEnd())
  )
  private def flip(implicit lang: Lang)       = row(kbd("f"), trans.flipBoard())
  private def zen(implicit lang: Lang)        = row(kbd("z"), trans.preferences.zenMode())
  private def helpDialog(implicit lang: Lang) = row(kbd("?"), trans.showHelpDialog())
  private def localAnalysis(implicit lang: Lang) = frag(
    row(kbd("l"), trans.toggleLocalAnalysis()),
    row(kbd("space"), trans.playComputerMove()),
    row(kbd("x"), trans.showThreat())
  )

  def round(implicit lang: Lang) =
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
  def puzzle(implicit lang: Lang) =
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
  def analyse(isStudy: Boolean)(implicit lang: Lang) =
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

  def keyboardMove(implicit lang: Lang) =
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
  def voiceMove(implicit lang: Lang) =
    import trans.keyboardMove.*
    frag(
      h2("Voice commands"),
      table(
        tbody(
          tr(th(colspan := 2)(p("Instructions"))),
          tr(
            td(colspan := 2)(
              ul(
                cls := "tips",
                li(
                  "Move pieces by speaking UCI, SAN, or the destination square"
                ),
                li(
                  "Say any piece (except \"pawn\") to move that piece " +
                    "or capture that opponent piece"
                ),
                li(
                  "Ambiguous requests result in colored arrows. " +
                    "Speak the color to choose or say \"clear\" to cancel"
                ),
                li(
                  "Up to 4 arrows are shown. Be more specific when more options exist"
                ),
                li(
                  "We sometimes include moves that sound like what you said"
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
            row(voice("e4"), "Move something to e4"),
            row(voice("knight"), "Move my knight or capture a knight"),
            row(voice("bishop g7"), "Move bishop to g7"),
            row(voice("queen takes rook"), "Take rook with queen"),
            row(voice("c8 promote knight"), "Move c pawn to 8, promote to knight"),
            row(voice("castle"), "Kingside castle"),
            row(voice("long castle"), "Queenside castle"),
            row(voice("b5c6"), "Full UCI or SAN is fine too")
          )
        ),
        table(
          tbody(
            header(otherCommands()),
            row(voice("draw"), offerOrAcceptDraw()),
            row(voice("resign"), trans.resignTheGame()),
            row(voice("clock"), readOutClocks()),
            row(voice("opponent"), readOutOpponentName()),
            row(voice("next"), trans.puzzle.nextPuzzle()),
            row(voice("up vote"), trans.puzzle.upVote()),
            row(voice("down vote"), trans.puzzle.downVote()),
            row(voice("help"), trans.showHelpDialog())
          )
        )
      )
    )
