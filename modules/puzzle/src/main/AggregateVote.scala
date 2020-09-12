package lila.puzzle

case class AggregateVote(up: Int, down: Int, nb: Int, ratio: Int) {

  def add(v: Boolean) =
    copy(
      up = up + (if (v) 1 else 0),
      down = down + (if (v) 0 else 1)
    ).computeNbAndRatio

  def change(from: Boolean, to: Boolean) =
    if (from == to) this
    else
      copy(
        up = up + (if (to) 1 else -1),
        down = down + (if (to) -1 else 1)
      ).computeNbAndRatio

  def count = up + down

  def sum = up - down

  def computeNbAndRatio =
    if (up + down > 0)
      copy(
        ratio = 100 * (up - down) / (up + down),
        nb = up + down
      )
    else
      copy(
        ratio = 1,
        nb = 0
      )
}

object AggregateVote {

  val default = AggregateVote(1, 0, 1, 100)
  val disable = AggregateVote(0, 9000, 9000, -100).computeNbAndRatio

  val minRatio = -50
  val minVotes = 30

  import reactivemongo.api.bson.Macros
  implicit val aggregatevoteBSONHandler = Macros.handler[AggregateVote]
}
