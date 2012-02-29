package lila.chess

case class Move(
  piece: Piece,
  orig: Pos,
  dest: Pos,
  before: Board,
  after: Board,
  capture: Option[Pos],
  promotion: Option[PromotableRole],
  castle: Boolean,
  enpassant: Boolean) {

    def withHistory(h: History) = copy(after = after withHistory h)

    // does this move capture an opponent piece?
    def captures = capture.isDefined

    def color = piece.color
}
