package lila.game

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.db.AsyncCollFailingSilently
import lila.db.dsl._
import lila.user.User

final class CrosstableApi(
    coll: AsyncCollFailingSilently,
    matchupColl: AsyncCollFailingSilently,
    enabled: () => Boolean
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
    justFetch(u1, u2) dmap { _ | Crosstable.empty(u1, u2) }

  def justFetch(u1: User.ID, u2: User.ID): Fu[Option[Crosstable]] = enabled() ??
    coll(_.one[Crosstable](select(u1, u2)))

  def withMatchup(u1: User.ID, u2: User.ID): Fu[Crosstable.WithMatchup] =
    apply(u1, u2) zip getMatchup(u1, u2) dmap { case (crosstable, matchup) =>
      Crosstable.WithMatchup(crosstable, matchup)
    }

  def nbGames(u1: User.ID, u2: User.ID): Fu[Int] = enabled() ??
    coll {
      _.find(
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
        val updateCrosstable = enabled() ?? coll {
          _.update
            .one(
              select(u1, u2),
              $inc(
                F.score1 -> inc1,
                F.score2 -> inc2
              ) ++ $push(
                Crosstable.BSONFields.results -> $doc(
                  "$each"  -> List(bsonResult),
                  "$slice" -> -Crosstable.maxGames
                )
              ),
              upsert = true
            )
            .void
        }
        val updateMatchup = matchupColl {
          _.update
            .one(
              select(u1, u2),
              $inc(
                F.score1 -> inc1,
                F.score2 -> inc2
              ) ++ $set(
                F.lastPlayed -> DateTime.now
              ),
              upsert = true
            )
            .void
        }
        updateCrosstable zip updateMatchup void
      case _ => funit
    }

  private val matchupProjection = $doc(F.lastPlayed -> false)

  def getMatchup(u1: User.ID, u2: User.ID): Fu[Option[Matchup]] =
    matchupColl(_.find(select(u1, u2), matchupProjection.some).one[Matchup])

  private def select(u1: User.ID, u2: User.ID) =
    $id(Crosstable.makeKey(u1, u2))
}
