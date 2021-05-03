package lila.coordinate

import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.user.User
import lila.db.dsl._

final class CoordinateApi(scoreColl: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val scoreBSONHandler = Macros.handler[Score]

  def getScore(userId: User.ID): Fu[Score] =
    scoreColl.byId[Score](userId) map (_ | Score(userId))

  def addScore(userId: User.ID, sente: Boolean, hits: Int): Funit =
    scoreColl.update
      .one(
        $id(userId),
        $push(
          $doc(
            "sente" -> BSONDocument(
              "$each"  -> (sente ?? List(BSONInteger(hits))),
              "$slice" -> -20
            ),
            "gote" -> BSONDocument(
              "$each"  -> (!sente ?? List(BSONInteger(hits))),
              "$slice" -> -20
            )
          )
        ),
        upsert = true
      )
      .void

  def bestScores(userIds: List[User.ID]): Fu[Map[User.ID, shogi.Color.Map[Int]]] =
    scoreColl
      .aggregateList(
        maxDocs = Int.MaxValue,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match($doc("_id" $in userIds)) -> List(
          Project(
            $doc(
              "sente" -> $doc("$max" -> "$sente"),
              "gote"  -> $doc("$max" -> "$gote")
            )
          )
        )
      }
      .map {
        _.flatMap { doc =>
          doc.string("_id") map {
            _ -> shogi.Color.Map(
              ~doc.int("sente"),
              ~doc.int("gote")
            )
          }
        }.toMap
      }
}
