package lila.relation

import play.api.libs.iteratee._
import reactivemongo.api.ReadPreference
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lila.common.MaxPerSecond
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class RelationStream(coll: Coll)(implicit system: akka.actor.ActorSystem) {

  import RelationStream._

  def follow(user: User, direction: Direction, perSecond: MaxPerSecond): Enumerator[User] = {
    val field = direction match {
      case Direction.Following => "u2"
      case Direction.Followers => "u1"
    }
    val projection = $doc(field -> true, "_id" -> false)
    val query = direction match {
      case Direction.Following => coll.find($doc("u1" -> user.id, "r" -> Follow), projection)
      case Direction.Followers => coll.find($doc("u2" -> user.id, "r" -> Follow), projection)
    }
    query.copy(options = query.options.batchSize(perSecond.value))
      .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
      .bulkEnumerator() &>
      lila.common.Iteratee.delay(1 second) &>
      Enumeratee.mapM { docs =>
        UserRepo usersFromSecondary docs.toSeq.flatMap(_.getAs[User.ID](field))
      } &>
      Enumeratee.mapConcat(_.toSeq)
  }
}

object RelationStream {

  sealed trait Direction
  object Direction {
    case object Following extends Direction
    case object Followers extends Direction
  }
}
