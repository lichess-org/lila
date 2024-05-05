package lila.challenge

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.id.ChallengeId

private def inTwoWeeks = nowInstant.plusWeeks(2)

val logger = lila.log("challenge")

case class AllChallenges(in: List[Challenge], out: List[Challenge]):
  def all = in ::: out

enum Direction:
  def name = Direction.this.toString.toLowerCase
  case In  // I can accept this challenge
  case Out // I created this challenge

object Event:
  case class Decline(c: Challenge)
  case class Cancel(c: Challenge)
