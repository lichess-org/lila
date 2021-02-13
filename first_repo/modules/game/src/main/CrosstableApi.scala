package lila.game

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User

final class CrosstableApi(
    coll: Coll,
    matchupColl: Coll
)(implicit ec: ExecutionContext) {

  import Crosstable.{ Matchup, Result }
  import Crosstable.{ BSONFields => F }

  def apply(game: Game): Fu[Option[Crosstable]] =
    game.twoUserIds ?? { case (u1, u2) =>
      apply(u1, u2) dmap some
    }

  def withMatchup(game: Game): Fu[Option[Crosstable.WithMatchup]] =
    game.twoUserIds ?? { case (u1, u2) =>
      withMatchup(u1, u2) dmap some
    }

  def apply(u1: User.ID, u2: User.ID): Fu[Crosstable] =
    justFetch(u1, u2) getOrElse create(u1, u2)

  def withMatchup(u1: User.ID, u2: User.ID): Fu[Crosstable.WithMatchup] =
    apply(u1, u2) zip getMatchup(u1, u2) dmap { case (crosstable, matchup) =>
      Crosstable.WithMatchup(crosstable, matchup)
    }

  def justFetch(u1: User.ID, u2: User.ID): Fu[Option[Crosstable]] =
    coll.one[Crosstable](select(u1, u2))

  def fetchOrEmpty(u1: User.ID, u2: User.ID): Fu[Crosstable] =
    justFetch(u1, u2) dmap { _ | Crosstable.empty(u1, u2) }

  def nbGames(u1: User.ID, u2: User.ID): Fu[Int] =
    coll
      .find(
        select(u1, u2),
        $doc("s1" -> true, "s2" -> true).some
      )
      .one[Bdoc] dmap { res =>
      ~(for {
        o  <- res
        s1 <- o.int("s1")
        s2 <- o.int("s2")
      } yield (s1 + s2) / 10)
    }

  def add(game: Game): Funit =
    game.userIds.distinct.sorted match {
      case List(u1, u2) =>
        val result     = Result(game.id, game.winnerUserId)
        val bsonResult = Crosstable.crosstableBSONHandler.writeResult(result, u1)
        def incScore(userId: User.ID): Int =
          game.winnerUserId match {
            case Some(u) if u == userId => 10
            case None                   => 5
            case _                      => 0
          }
        val inc1 = incScore(u1)
        val inc2 = incScore(u2)
        val updateCrosstable = coll.update.one(
          select(u1, u2),
          $inc(
            F.score1 -> inc1,
            F.score2 -> inc2
          ) ++ $push(
            Crosstable.BSONFields.results -> $doc(
              "$each"  -> List(bsonResult),
              "$slice" -> -Crosstable.maxGames
            )
          )
        )
        val updateMatchup =
          matchupColl.update.one(
            select(u1, u2),
            $inc(
              F.score1 -> inc1,
              F.score2 -> inc2
            ) ++ $set(
              F.lastPlayed -> DateTime.now
            ),
            upsert = true
          )
        updateCrosstable zip updateMatchup void
      case _ => funit
    }

  private val matchupProjection = $doc(F.lastPlayed -> false)

  def getMatchup(u1: User.ID, u2: User.ID): Fu[Option[Matchup]] =
    matchupColl.find(select(u1, u2), matchupProjection.some).one[Matchup]

  private def create(u1: User.ID, u2: User.ID): Fu[Crosstable] = {
    val crosstable = Crosstable.empty(u1, u2)
    coll.insert.one(crosstable) recover lila.db.recoverDuplicateKey(_ => ()) inject crosstable
  }

  private def select(u1: User.ID, u2: User.ID) =
    $id(Crosstable.makeKey(u1, u2))
}
