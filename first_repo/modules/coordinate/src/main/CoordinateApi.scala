package lila.coordinate

import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.user.User
import lila.db.dsl._

final class CoordinateApi(scoreColl: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val scoreBSONHandler = Macros.handler[Score]

  def getScore(userId: User.ID): Fu[Score] =
    scoreColl.byId[Score](userId) map (_ | Score(userId))

  def addScore(userId: User.ID, white: Boolean, hits: Int): Funit =
    scoreColl.update
      .one(
        $id(userId),
        $push(
          $doc(
            "white" -> BSONDocument(
              "$each"  -> (white ?? List(BSONInteger(hits))),
              "$slice" -> -20
            ),
            "black" -> BSONDocument(
              "$each"  -> (!white ?? List(BSONInteger(hits))),
              "$slice" -> -20
            )
          )
        ),
        upsert = true
      )
      .void

  def bestScores(userIds: List[User.ID]): Fu[Map[User.ID, chess.Color.Map[Int]]] =
    scoreColl
      .aggregateList(
        maxDocs = Int.MaxValue,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match($doc("_id" $in userIds)) -> List(
          Project(
            $doc(
              "white" -> $doc("$max" -> "$white"),
              "black" -> $doc("$max" -> "$black")
            )
          )
        )
      }
      .map {
        _.flatMap { doc =>
          doc.string("_id") map {
            _ -> chess.Color.Map(
              ~doc.int("white"),
              ~doc.int("black")
            )
          }
        }.toMap
      }
}
