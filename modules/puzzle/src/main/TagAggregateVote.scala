package lila.puzzle

case class TagAggregateVote(up: Int, down: Int) {

  def sum = up - down
}
