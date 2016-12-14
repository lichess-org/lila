package lila.puzzle

case class TagVoted(tag: Tag, vote: TagAggregateVote) {

  def trusted: Boolean = vote.sum > 3

  def visible = vote.sum > 2
}

case class TagVoteds(value: List[TagVoted])
