package lila.pool

import akka.actor._
import org.joda.time.DateTime

import lila.user.User

private final class PoolActor(
    config: PoolConfig,
    gameStarter: GameStarter) extends Actor {

  import PoolActor._

  var members = Vector[PoolMember]()

  def receive = {

    case Join(joiner) =>
      members = members.filter(_.userId != joiner.userId) :+ PoolMember(joiner, config)
      if (members.size > 1) self ! MakePairings

    case Leave(userId) =>
      members = members.filter(_.userId != userId)

    case MakePairings if members.size > 1 =>
      val pairings = MatchMaking(members)
      members = members diff pairings.flatMap(_.members)
      gameStarter(config, pairings)
      sender ! pairings
  }
}

private object PoolActor {

  case class Join(joiner: PoolApi.Joiner) extends AnyVal
  case class Leave(userId: User.ID) extends AnyVal

  case object MakePairings

}
