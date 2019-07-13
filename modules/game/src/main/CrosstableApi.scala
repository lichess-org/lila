package lila.game

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class CrosstableApi(
    coll: Coll,
    matchupColl: Coll,
    gameColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: akka.actor.ActorSystem
) {

  import Crosstable.{ Matchup, Result }
  import Crosstable.{ BSONFields => F }
  import Game.{ BSONFields => GF }

  def apply(game: Game): Fu[Option[Crosstable]] = game.userIds.distinct match {
    case List(u1, u2) => apply(u1, u2)
    case _ => fuccess(none)
  }

  def withMatchup(game: Game): Fu[Option[Crosstable.WithMatchup]] = game.userIds.distinct match {
    case List(u1, u2) => withMatchup(u1, u2)
    case _ => fuccess(none)
  }

  def apply(u1: User.ID, u2: User.ID, timeout: FiniteDuration = 1.second): Fu[Option[Crosstable]] =
    coll.uno[Crosstable](select(u1, u2)) orElse createWithTimeout(u1, u2, timeout)

  def withMatchup(u1: User.ID, u2: User.ID, timeout: FiniteDuration = 1.second): Fu[Option[Crosstable.WithMatchup]] =
    apply(u1, u2, timeout) zip getMatchup(u1, u2) map {
      case crosstable ~ matchup => crosstable.map { Crosstable.WithMatchup(_, matchup) }
    }

  def nbGames(u1: User.ID, u2: User.ID): Fu[Int] =
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
    case List(u1, u2) => {
      val result = Result(game.id, game.winnerUserId)
      val bsonResult = Crosstable.crosstableBSONHandler.writeResult(result, u1)
      def incScore(userId: User.ID): Int = game.winnerUserId match {
        case Some(u) if u == userId => 10
        case None => 5
        case _ => 0
      }
      val inc1 = incScore(u1)
      val inc2 = incScore(u2)
      val updateCrosstable = coll.update(select(u1, u2), $inc(
        F.score1 -> inc1,
        F.score2 -> inc2
      ) ++ $push(
          Crosstable.BSONFields.results -> $doc(
            "$each" -> List(bsonResult),
            "$slice" -> -Crosstable.maxGames
          )
        ))
      val updateMatchup =
        matchupColl.update(select(u1, u2), $inc(
          F.score1 -> inc1,
          F.score2 -> inc2
        ) ++ $set(
            F.lastPlayed -> DateTime.now
          ), upsert = true)
      updateCrosstable zip updateMatchup void
    }
    case _ => funit
  }

  private val matchupProjection = $doc(F.lastPlayed -> false)

  private def getMatchup(u1: User.ID, u2: User.ID): Fu[Option[Matchup]] =
    matchupColl.find(select(u1, u2), matchupProjection).uno[Matchup]

  private def createWithTimeout(u1: User.ID, u2: User.ID, timeout: FiniteDuration) =
    creationCache.get(u1 -> u2).withTimeoutDefault(timeout, none)(system)

  // to avoid creating it twice during a new matchup
  private val creationCache = asyncCache.multi[(User.ID, User.ID), Option[Crosstable]](
    name = "crosstable",
    f = (create _).tupled,
    resultTimeout = 19.second,
    expireAfter = _.ExpireAfterWrite(20 seconds)
  )

  private val winnerProjection = $doc(GF.winnerId -> true)

  private def create(x1: User.ID, x2: User.ID): Fu[Option[Crosstable]] = {
    UserRepo.orderByGameCount(x1, x2) map (_ -> List(x1, x2).sorted) flatMap {
      case (Some((u1, u2)), List(su1, su2)) =>
        val selector = $doc(
          GF.playerUids $all List(u1, u2),
          GF.status $gte chess.Status.Mate.id
        )

        import reactivemongo.api.ReadPreference

        gameColl.find(selector, winnerProjection)
          .sort($doc(GF.createdAt -> -1))
          .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
          .gather[List]().map { docs =>

            val (s1, s2) = docs.foldLeft(0 -> 0) {
              case ((s1, s2), doc) => doc.getAs[User.ID](GF.winnerId) match {
                case Some(u) if u == su1 => (s1 + 10, s2)
                case Some(u) if u == su2 => (s1, s2 + 10)
                case _ => (s1 + 5, s2 + 5)
              }
            }
            Crosstable(
              Crosstable.Users(
                Crosstable.User(su1, s1),
                Crosstable.User(su2, s2)
              ),
              results = docs.take(Crosstable.maxGames).flatMap { doc =>
                doc.getAs[String](GF.id).map { id =>
                  Result(id, doc.getAs[User.ID](GF.winnerId))
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

  private def select(u1: User.ID, u2: User.ID) =
    $id(Crosstable.makeKey(u1, u2))
}
