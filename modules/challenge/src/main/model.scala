package lila.challenge

case class AllChallenges(in: List[Challenge], out: List[Challenge]) {
  def all = in ::: out
}

sealed trait Direction {
  val name = toString.toLowerCase
}
object Direction {
  case object In  extends Direction // I can accept this challenge
  case object Out extends Direction // I created this challenge
}

object Event {
  case class Create(c: Challenge)
  case class Accept(c: Challenge, joinerId: Option[String])
  case class Decline(c: Challenge)
  case class Cancel(c: Challenge)
}
