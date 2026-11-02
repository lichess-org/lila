package lila.coordinate

import chess.{ ByColor, Color }
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class CoordinateApi(scoreColl: Coll)(using Executor):

  private given BSONDocumentHandler[Score] = Macros.handler[Score]

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    scoreColl.delete.one(bid(del.id)).void

  def getScore(userId: UserId): Fu[Score] =
    scoreColl.byId[Score](userId).dmap(_ | Score(userId))

  def addScore(mode: CoordMode, color: Color, hits: Int)(using me: MyId): Funit =
    scoreColl.update
      .one(
        bid(me),
        push(
          bdoc(
            s"${color.name}${(mode == CoordMode.nameSquare).so("NameSquare")}" -> bdoc(
              "$each" -> barr(hits),
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
        Match(bdoc("_id".in(userIds))) -> List(
          Project(
            bdoc(
              "white" -> bdoc("$max" -> "$white"),
              "black" -> bdoc("$max" -> "$black")
            )
          )
        )
      .map:
        _.flatMap: doc =>
          doc
            .getAsOpt[UserId]("_id")
            .map:
              _ -> ByColor(~doc.int("white"), ~doc.int("black"))
        .toMap
