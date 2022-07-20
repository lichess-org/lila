package views.html.board

import shogi.format.forsyth.Sfen

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  private val dataState = attr("data-state")

  def mini(sfen: Sfen, color: shogi.Color = shogi.Sente, lastMove: String = "")(tag: Tag): Tag =
    tag(
      cls       := "mini-board mini-board--init sg-wrap",
      dataState := s"${sfen.value},${color.name},$lastMove"
    )(sgWrapContent)

  def miniSpan(sfen: Sfen, color: shogi.Color = shogi.Sente, lastMove: String = "") =
    mini(sfen, color, lastMove)(span)

}
