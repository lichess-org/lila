package lila.relation

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ given, * }
import lila.user.{ User, UserRepo }

final class RelationStream(colls: Colls, userRepo: UserRepo)(using akka.stream.Materializer):

  import RelationStream.*

  private val coll = colls.relation

  def follow(user: User, direction: Direction, perSecond: MaxPerSecond): Source[User, ?] =
    coll
      .find(
        $doc(selectField(direction) -> user.id, "r" -> Follow),
        $doc(projectField(direction) -> true, "_id" -> false).some
      )
      .batchSize(perSecond.value)
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(_.getAsOpt[UserId](projectField(direction))))
      .throttle(1, 1 second)
      .mapAsync(1)(userRepo.usersFromSecondary)
      .mapConcat(identity)

  private def selectField(d: Direction) = d match
    case Direction.Following => "u1"
    case Direction.Followers => "u2"
  private def projectField(d: Direction) = d match
    case Direction.Following => "u2"
    case Direction.Followers => "u1"

object RelationStream:

  enum Direction:
    case Following, Followers
