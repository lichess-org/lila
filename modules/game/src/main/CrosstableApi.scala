package lila.game

import lila.db.Types._
import lila.user.UserRepo

import reactivemongo.core.commands.Count

import reactivemongo.bson.{ BSONDocument, BSONInteger }

final class CrosstableApi(coll: Coll) {

  import Crosstable.Result

  private val maxGames = 20

  def apply(game: Game): Fu[Option[Crosstable]] = game.userIds.distinct match {
    case List(u1, u2) => apply(u1, u2)
    case _            => fuccess(none)
  }

  def apply(u1: String, u2: String): Fu[Option[Crosstable]] =
    coll.find(select(u1, u2)).one[Crosstable] orElse create(u1, u2) recover {
      case e: reactivemongo.core.commands.LastError if e.getMessage.contains("duplicate key error") => none
    }

  def add(game: Game): Funit = game.userIds.distinct.sorted match {
    case List(u1, u2) =>
      val result = Result(game.id, game.winnerUserId)
      val bsonResult = Crosstable.crosstableBSONHandler.writeResult(result, u1)
      val bson = BSONDocument(
        "$inc" -> BSONDocument(Crosstable.BSONFields.nbGames -> BSONInteger(1))
      ) ++ {
          if (game.rated) BSONDocument("$push" -> BSONDocument(
            Crosstable.BSONFields.results -> BSONDocument(
              "$each" -> List(bsonResult),
              "$slice" -> -maxGames
            )))
          else BSONDocument()
        }
      coll.update(select(u1, u2), bson).void
    case _ => funit
  }

  private def exists(u1: String, u2: String) =
    coll.db command Count(coll.name, select(u1, u2).some) map (0 !=)

  private def create(x1: String, x2: String): Fu[Option[Crosstable]] =
    UserRepo.orderByGameCount(x1, x2) map (_ -> List(x1, x2).sorted) flatMap {
      case (Some((u1, u2)), List(su1, su2)) => {
        val selector = BSONDocument(
          Game.BSONFields.playerUids -> BSONDocument("$all" -> List(u1, u2)),
          Game.BSONFields.status -> BSONDocument("$gte" -> chess.Status.Mate.id))
        tube.gameTube.coll.find(
          selector,
          BSONDocument(Game.BSONFields.winnerId -> true)
        ).sort(BSONDocument(Game.BSONFields.createdAt -> -1))
          .cursor[BSONDocument].collect[List](maxGames).map {
            _.map { doc =>
              doc.getAs[String](Game.BSONFields.id).map { id =>
                Result(id, doc.getAs[String](Game.BSONFields.winnerId))
              }
            }.flatten.reverse
          } zip (tube.gameTube.coll.db command Count(tube.gameTube.coll.name, selector.some)) map {
            case (results, nbGames) => Crosstable(su1, su2, results, nbGames)
          }
      } flatMap { crosstable =>
        coll insert crosstable inject crosstable.some
      }
      case _ => fuccess(none)
    }

  private def select(u1: String, u2: String) = BSONDocument("_id" -> Crosstable.makeKey(u1, u2))
}
