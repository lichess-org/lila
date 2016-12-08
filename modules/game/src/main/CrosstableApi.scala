package lila.game

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.AsyncCache
import lila.user.UserRepo

final class CrosstableApi(
    coll: Coll,
    gameColl: Coll,
    system: akka.actor.ActorSystem) {

  import Crosstable.Result

  private val maxGames = 20

  def apply(game: Game): Fu[Option[Crosstable]] = game.userIds.distinct match {
    case List(u1, u2) => apply(u1, u2)
    case _            => fuccess(none)
  }

  def apply(u1: String, u2: String): Fu[Option[Crosstable]] =
    coll.uno[Crosstable](select(u1, u2)) orElse createFast(u1, u2)

  def nbGames(u1: String, u2: String): Fu[Int] =
    coll.find(
      select(u1, u2),
      $doc("s1" -> true, "s2" -> true)
    ).uno[Bdoc] map { res =>
        ~(for {
          o <- res
          s1 <- o.getAs[Int]("s1")
          s2 <- o.getAs[Int]("s2")
        } yield (s1 + s2) / 10)
      }

  def add(game: Game): Funit = game.userIds.distinct.sorted match {
    case List(u1, u2) =>
      val result = Result(game.id, game.winnerUserId)
      val bsonResult = Crosstable.crosstableBSONHandler.writeResult(result, u1)
      def incScore(userId: String) = $int(game.winnerUserId match {
        case Some(u) if u == userId => 10
        case None                   => 5
        case _                      => 0
      })
      val bson = $doc(
        "$inc" -> $doc(
          "s1" -> incScore(u1),
          "s2" -> incScore(u2))
      ) ++ $doc("$push" -> $doc(
          Crosstable.BSONFields.results -> $doc(
            "$each" -> List(bsonResult),
            "$slice" -> -maxGames
          )))
      coll.update(select(u1, u2), bson).void
    case _ => funit
  }

  private def exists(u1: String, u2: String) =
    coll.exists(select(u1, u2))

  private def createFast(u1: String, u2: String) =
    creationCache(u1 -> u2).withTimeoutDefault(1 second, none)(system)

  // to avoid creating it twice during a new matchup
  private val creationCache = AsyncCache[(String, String), Option[Crosstable]](
    name = "crosstable",
    f = (create _).tupled,
    timeToLive = 20 seconds)

  private val winnerProjection = $doc(Game.BSONFields.winnerId -> true)

  private def create(x1: String, x2: String): Fu[Option[Crosstable]] = {
    UserRepo.orderByGameCount(x1, x2) map (_ -> List(x1, x2).sorted) flatMap {
      case (Some((u1, u2)), List(su1, su2)) =>
        val selector = $doc(
          Game.BSONFields.playerUids $all List(u1, u2),
          Game.BSONFields.status $gte chess.Status.Mate.id)

        import reactivemongo.api.ReadPreference

        gameColl.find(selector, winnerProjection)
          .sort($doc(Game.BSONFields.createdAt -> -1))
          .cursor[Bdoc](readPreference = ReadPreference.secondary)
          .gather[List]().map { docs =>

            val (s1, s2) = docs.foldLeft(0 -> 0) {
              case ((s1, s2), doc) => doc.getAs[String](Game.BSONFields.winnerId) match {
                case Some(u) if u == su1 => (s1 + 10, s2)
                case Some(u) if u == su2 => (s1, s2 + 10)
                case _                   => (s1 + 5, s2 + 5)
              }
            }
            Crosstable(
              Crosstable.User(su1, s1),
              Crosstable.User(su2, s2),
              results = docs.take(maxGames).flatMap { doc =>
                doc.getAs[String](Game.BSONFields.id).map { id =>
                  Result(id, doc.getAs[String](Game.BSONFields.winnerId))
                }
              }.reverse
            )
          } flatMap { crosstable =>
            coll insert crosstable inject crosstable.some
          }

      case _ => fuccess(none)
    }
  } recoverWith lila.db.recoverDuplicateKey { _ =>
    coll.uno[Crosstable](select(x1, x2))
  } recover {
    case e: Exception =>
      logger.error("CrosstableApi.create", e)
      none
  }

  private def select(u1: String, u2: String) =
    $id(Crosstable.makeKey(u1, u2))
}
