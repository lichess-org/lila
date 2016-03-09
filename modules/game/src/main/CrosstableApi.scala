package lila.game

import play.api.libs.json.JsObject
import reactivemongo.bson.{ BSONDocument, BSONInteger }
import reactivemongo.core.commands._

import lila.common.PimpedJson._
import lila.db.Types._
import lila.user.UserRepo

final class CrosstableApi(coll: Coll) {

  import Crosstable.Result

  private val maxGames = 20

  def apply(game: Game): Fu[Option[Crosstable]] = game.userIds.distinct match {
    case List(u1, u2) => apply(u1, u2)
    case _            => fuccess(none)
  }

  def apply(u1: String, u2: String): Fu[Option[Crosstable]] =
    coll.find(select(u1, u2)).one[Crosstable] orElse create(u1, u2) recoverWith
      lila.db.recoverDuplicateKey(_ => coll.find(select(u1, u2)).one[Crosstable])

  def nbGames(u1: String, u2: String): Fu[Int] =
    coll.find(
      select(u1, u2),
      BSONDocument("n" -> true)
    ).one[BSONDocument] map {
        ~_.flatMap(_.getAs[Int]("n"))
      }

  def add(game: Game): Funit = game.userIds.distinct.sorted match {
    case List(u1, u2) =>
      val result = Result(game.id, game.winnerUserId)
      val bsonResult = Crosstable.crosstableBSONHandler.writeResult(result, u1)
      val bson = BSONDocument(
        "$inc" -> BSONDocument(
          Crosstable.BSONFields.nbGames -> BSONInteger(1),
          "s1" -> BSONInteger(game.winnerUserId match {
            case Some(u) if u == u1 => 10
            case None               => 5
            case _                  => 0
          }),
          "s2" -> BSONInteger(game.winnerUserId match {
            case Some(u) if u == u2 => 10
            case None               => 5
            case _                  => 0
          })
        )
      ) ++ BSONDocument("$push" -> BSONDocument(
          Crosstable.BSONFields.results -> BSONDocument(
            "$each" -> List(bsonResult),
            "$slice" -> -maxGames
          )))
      coll.update(select(u1, u2), bson).void
    case _ => funit
  }

  private def exists(u1: String, u2: String) =
    coll.count(select(u1, u2).some) map (0 !=)

  private def create(x1: String, x2: String): Fu[Option[Crosstable]] =
    UserRepo.orderByGameCount(x1, x2) map (_ -> List(x1, x2).sorted) flatMap {
      case (Some((u1, u2)), List(su1, su2)) =>
        val gameColl = tube.gameTube.coll

        val selector = BSONDocument(
          Game.BSONFields.playerUids -> BSONDocument("$all" -> List(u1, u2)),
          Game.BSONFields.status -> BSONDocument("$gte" -> chess.Status.Mate.id))

        import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, SumValue, GroupField }
        import reactivemongo.api.ReadPreference

        for {
          localResults <- gameColl.find(selector,
            BSONDocument(Game.BSONFields.winnerId -> true)
          ).sort(BSONDocument(Game.BSONFields.createdAt -> -1))
            .cursor[BSONDocument](readPreference = ReadPreference.secondaryPreferred)
            .collect[List](maxGames).map {
              _.flatMap { doc =>
                doc.getAs[String](Game.BSONFields.id).map { id =>
                  Result(id, doc.getAs[String](Game.BSONFields.winnerId))
                }
              }.reverse
            }
          nbGames <- gameColl.count(selector.some)
          ctDraft = Crosstable(Crosstable.User(su1, 0), Crosstable.User(su2, 0), localResults, nbGames)

          crosstable <- gameColl.aggregate(Match(selector), List(
            GroupField(Game.BSONFields.winnerId)("nb" -> SumValue(1)))).map(
            _.firstBatch.foldLeft(ctDraft) {
              case (ct, obj) => obj.getAs[Int]("nb").fold(ct) { nb =>
                ct.addWins(obj.getAs[String]("_id"), nb)
              }
            }
          )

          _ <- coll insert crosstable
        } yield crosstable.some

      case _ => fuccess(none)
    }

  private def select(u1: String, u2: String) =
    BSONDocument("_id" -> Crosstable.makeKey(u1, u2))
}
