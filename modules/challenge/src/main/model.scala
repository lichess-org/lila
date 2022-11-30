package lila.challenge

case class AllChallenges(in: List[Challenge], out: List[Challenge]):
  def all = in ::: out

enum Direction:
  def name = Direction.this.toString.toLowerCase
  case In  // I can accept this challenge
  case Out // I created this challenge

object Event:
  case class Create(c: Challenge)
  case class Accept(c: Challenge, joinerId: Option[UserId])
  case class Decline(c: Challenge)
  case class Cancel(c: Challenge)
