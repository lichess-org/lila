package lila.tutor

case class NbGames(value: Int) extends AnyVal with IntValue {
  def +(inc: Int)        = NbGames(value + inc)
  def inc(cond: Boolean) = if (cond) this + 1 else this
}
case class NbMoves(value: Int) extends AnyVal with IntValue {
  def +(inc: Int) = NbMoves(value + inc)
}
case class NbMovesRatio(a: Int, b: Int)
