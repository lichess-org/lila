package lila.relation

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.db.dsl.{ *, given }

final class RelationStream(colls: Colls)(using akka.stream.Materializer):

  import RelationStream.*

  private val coll = colls.relation

  def follow(userId: UserId, direction: Direction, perSecond: MaxPerSecond): Source[Seq[UserId], ?] =
    coll
      .find(
        $doc(selectField(direction) -> userId, "r" -> lila.core.relation.Relation.Follow),
        $doc(projectField(direction) -> true, "_id" -> false).some
      )
      .batchSize(perSecond.value)
      .cursor[Bdoc](ReadPref.sec)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(_.getAsOpt[UserId](projectField(direction))))
      .throttle(1, 1.second)

  private def selectField(d: Direction) = d match
    case Direction.Following => "u1"
    case Direction.Followers => "u2"
  private def projectField(d: Direction) = d match
    case Direction.Following => "u2"
    case Direction.Followers => "u1"

object RelationStream:

  enum Direction:
    case Following, Followers
