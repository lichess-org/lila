package lila.pool

import akka.actor._
import org.joda.time.DateTime

import lila.user.User

private final class PoolActor(config: PoolConfig) extends Actor {

  import PoolActor._

  var members = Vector[PoolMember]()

  def receive = {

    case Join(user) =>
      members = members.filter(_.userId != user.id) :+ PoolMember(user, config)

    case Leave(userId) =>
      members = members.filter(_.userId != userId)

    case MakePairings =>
      val pairings = MatchMaking(members)
      members = members diff pairings.flatMap(_.members)
      sender ! pairings
  }
}

private object PoolActor {

  case class Join(user: User) extends AnyVal
  case class Leave(userId: User.ID) extends AnyVal

  case object MakePairings

}
