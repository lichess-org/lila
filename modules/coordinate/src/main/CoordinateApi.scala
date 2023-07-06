package lila.coordinate

import reactivemongo.api.bson.*

import lila.db.dsl.{ given, * }
import chess.{ Color, ByColor }
import lila.user.Me

final class CoordinateApi(scoreColl: Coll)(using Executor):

  private given BSONDocumentHandler[Score] = Macros.handler[Score]

  def getScore(userId: UserId): Fu[Score] =
    scoreColl.byId[Score](userId).dmap(_ | Score(userId))

  def addScore(mode: CoordMode, color: Color, hits: Int)(using me: Me.Id): Funit =
    scoreColl.update
      .one(
        $id(me),
        $push(
          $doc(
            s"${color.name}${(mode == CoordMode.nameSquare) so "NameSquare"}" -> $doc(
              "$each"  -> $arr(hits),
              "$slice" -> -20
            )
          )
        ),
        upsert = true
      )
      .void

  def bestScores(userIds: List[UserId]): Fu[Map[UserId, ByColor[Int]]] =
    scoreColl
      .aggregateList(maxDocs = Int.MaxValue, _.sec): framework =>
        import framework.*
        Match($doc("_id" $in userIds)) -> List(
          Project(
            $doc(
              "white" -> $doc("$max" -> "$white"),
              "black" -> $doc("$max" -> "$black")
            )
          )
        )
      .map:
        _.flatMap: doc =>
          doc.getAsOpt[UserId]("_id") map {
            _ -> ByColor(~doc.int("white"), ~doc.int("black"))
          }
        .toMap
