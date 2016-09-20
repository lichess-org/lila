package lila.puzzle

case class AggregateVote(up: Int, down: Int, sum: Int) {

  def add(v: Boolean) = copy(
    up = up + v.fold(1, 0),
    down = down + v.fold(0, 1)
  ).computeSum

  def change(from: Boolean, to: Boolean) = if (from == to) this else copy(
    up = up + to.fold(1, -1),
    down = down + to.fold(-1, 1)
  ).computeSum

  def count = up + down

  def computeSum = copy(sum = up - down)
}

object AggregateVote {

  val default = AggregateVote(0, 0, 0)
  val disable = AggregateVote(0, 9000, 0).computeSum

  import reactivemongo.bson.Macros
  implicit val aggregatevoteBSONHandler = Macros.handler[AggregateVote]
}
