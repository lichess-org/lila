package views.html.keyboardMove

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

  def apply()(implicit ctx: Context) =
    frag(
      h2(trans.keyboardMove.keyboardInputCommands()),
      table(
        tbody(
          header(trans.keyboardMove.performAMove()),
          row(frag(k("e2e4")), trans.keyboardMove.movePieceFromE2ToE4()),
          row(frag(k("Nc3")), trans.keyboardMove.moveKnightToC3()),
          row(frag(k("O-O")), trans.keyboardMove.kingsideCastle()),
          row(frag(k("O-O-O")), trans.keyboardMove.queensideCastle()),
          row(frag(k("c8=Q")), trans.keyboardMove.promoteC8ToQueen()),
          row(frag(k("R@b4")), trans.keyboardMove.dropARookAtB4()),
          header(trans.keyboardMove.otherCommands()),
          row(frag(k("/")), trans.focusChat()),
          row(frag(k("clock")), trans.keyboardMove.readOutClocks()),
          row(frag(k("draw")), trans.keyboardMove.offerOrAcceptDraw()),
          row(frag(k("resign")), trans.resignTheGame()),
          row(frag(k("help"), or, k("?")), trans.showHelpDialog()),
          header(trans.keyboardMove.tips()),
          tr(
            td(cls := "tips", colspan := 2)(
              ul(
                li(
                  trans.keyboardMove.ifTheAboveMoveNotationIsUnfamiliar(),
                  a(target := "_blank", href := "https://en.wikipedia.org/wiki/Algebraic_notation_(chess)")(
                    "Algebraic notation (chess)"
                  )
                ),
                li(trans.keyboardMove.includingAXToIndicateACapture()),
                li(trans.keyboardMove.bothTheLetterOAndTheDigitZero()),
                li(trans.keyboardMove.ifItIsLegalToCastleBothWays()),
                li(trans.keyboardMove.capitalizationOnlyMattersInAmbiguousSituations()),
                li(trans.keyboardMove.toPremoveSimplyTypeTheDesiredPremove())
              )
            )
          )
        )
      )
    )
}
