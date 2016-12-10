package lila.puzzle

case class AggregateVote(up: Int, down: Int, enabled: Boolean) {

  def add(v: Boolean) = copy(
    up = up + v.fold(1, 0),
    down = down + v.fold(0, 1)
  ).computeEnabled

  def change(from: Boolean, to: Boolean) = if (from == to) this else copy(
    up = up + to.fold(1, -1),
    down = down + to.fold(-1, 1)
  ).computeEnabled

  def count = up + down

  def sum = up - down

  def computeEnabled = copy(enabled = count < 50 || up * 3 > down)
}

object AggregateVote {

  val default = AggregateVote(1, 0, true)
  val disable = AggregateVote(0, 9000, false).computeEnabled

  import reactivemongo.bson.Macros
  implicit val aggregatevoteBSONHandler = Macros.handler[AggregateVote]
}
