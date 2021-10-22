package lila.tutor

case class NbGames(value: Int) extends AnyVal with IntValue {
  def +(inc: Int) = NbGames(value + inc)
}
case class NbMoves(value: Int) extends AnyVal with IntValue {
  def +(inc: Int) = NbMoves(value + inc)
}
case class NbMovesRatio(a: Int, b: Int)
