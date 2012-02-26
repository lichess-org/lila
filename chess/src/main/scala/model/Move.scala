package lila.chess
package model

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
}
