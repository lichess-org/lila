package lila.coordinate

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.user.User
import lila.db.dsl.{ given, * }
import chess.Color

final class CoordinateApi(scoreColl: Coll)(using ec: scala.concurrent.ExecutionContext):

  private given BSONDocumentHandler[Score] = Macros.handler[Score]

  def getScore(userId: UserId): Fu[Score] =
    scoreColl.byId[Score](userId).dmap(_ | Score(userId))

  def addScore(userId: UserId, mode: CoordMode, color: Color, hits: Int): Funit =
    scoreColl.update
      .one(
        $id(userId),
        $push(
          $doc(
            s"${color.name}${(mode == CoordMode.NameSquare) ?? "NameSquare"}" -> $doc(
              "$each"  -> $arr(hits),
              "$slice" -> -20
            )
          )
        ),
        upsert = true
      )
      .void

  def bestScores(userIds: List[UserId]): Fu[Map[UserId, Color.Map[Int]]] =
    scoreColl
      .aggregateList(
        maxDocs = Int.MaxValue,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework.*
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
          doc.getAsOpt[UserId]("_id") map {
            _ -> Color.Map(
              ~doc.int("white"),
              ~doc.int("black")
            )
          }
        }.toMap
      }
