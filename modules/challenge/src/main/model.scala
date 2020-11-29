package lila.challenge

case class AllChallenges(in: List[Challenge], out: List[Challenge])

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
  case class Decline(c: Challenge, reason: Option[String])
  case class Cancel(c: Challenge)
}
