package lila.relation

import akka.stream.scaladsl._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.config.MaxPerSecond
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class RelationStream(
    coll: Coll,
    userRepo: UserRepo
)(implicit mat: akka.stream.Materializer) {

  import RelationStream._

  def follow(user: User, direction: Direction, perSecond: MaxPerSecond): Source[User, _] =
    coll
      .find(
        $doc(selectField(direction) -> user.id, "r" -> Follow),
        $doc(projectField(direction) -> true, "_id" -> false).some
      )
      .batchSize(perSecond.value)
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(_.getAsOpt[User.ID](projectField(direction))))
      .throttle(1, 1 second)
      .mapAsync(1)(userRepo.usersFromSecondary)
      .mapConcat(identity)

  private def selectField(d: Direction) =
    d match {
      case Direction.Following => "u1"
      case Direction.Followers => "u2"
    }
  private def projectField(d: Direction) =
    d match {
      case Direction.Following => "u2"
      case Direction.Followers => "u1"
    }
}

object RelationStream {

  sealed trait Direction
  object Direction {
    case object Following extends Direction
    case object Followers extends Direction
  }
}
