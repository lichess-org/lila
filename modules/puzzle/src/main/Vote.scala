package lila.puzzle

case class Vote(up: Int, down: Int, sum: Int) {

  def add(v: Boolean) = copy(
    up = up + v.fold(1, 0),
    down = down + v.fold(0, 1)
  ).computeSum

  def change(from: Boolean, to: Boolean) = if (from == to) this else copy(
    up = up + to.fold(1, -1),
    down = down + to.fold(-1, 1)
  ).computeSum

  def count = up + down

  def percent = 50 + (sum.toDouble / count * 50).toInt

  def computeSum = copy(sum = up - down)
}

object Vote {

  val default = Vote(1, 0, 1)
  val disable = Vote(0, 9000, 0).computeSum

  import reactivemongo.bson.Macros
  implicit val voteBSONHandler = Macros.handler[Vote]
}
