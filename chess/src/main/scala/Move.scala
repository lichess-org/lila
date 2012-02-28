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

    def situation = before as piece.color

    // does this move check the opponent?
    def checks: Boolean = (after as !color).check

    // does this move checkmate the opponent?
    def checkMates: Boolean = (after as !color).checkMate

    // does this move capture an opponent piece?
    def captures = capture.isDefined

    def color = piece.color
}
