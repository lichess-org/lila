package lila.game

import lila.core.game.Game
import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }

final class CrosstableApi(
    coll: Coll,
    matchupColl: AsyncCollFailingSilently
)(using Executor):

  import Crosstable.{ Matchup, Result }
  import Crosstable.BSONFields as F

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    matchupColl:
      _.delete.one($doc("_id".$startsWith(s"${del.id}/"))).void

  def apply(game: Game): Fu[Option[Crosstable]] =
    game.twoUserIds.soFu(apply.tupled)

  def withMatchup(game: Game): Fu[Option[Crosstable.WithMatchup]] =
    game.twoUserIds.soFu(withMatchup.tupled)

  def apply(u1: UserId, u2: UserId): Fu[Crosstable] =
    justFetch(u1, u2).dmap(_ | Crosstable.empty(u1, u2))

  def justFetch(u1: UserId, u2: UserId): Fu[Option[Crosstable]] =
    coll.one[Crosstable](select(u1, u2))

  def withMatchup(u1: UserId, u2: UserId): Fu[Crosstable.WithMatchup] =
    apply(u1, u2).zip(getMatchup(u1, u2)).dmap(Crosstable.WithMatchup.apply.tupled)

  def nbGames(u1: UserId, u2: UserId): Fu[Int] =
    coll
      .find(
        select(u1, u2),
        $doc("s1" -> true, "s2" -> true).some
      )
      .one[Bdoc]
      .dmap: res =>
        ~(for
          o <- res
          s1 <- o.int("s1")
          s2 <- o.int("s2")
        yield (s1 + s2) / 10)

  def add(game: Game): Funit =
    game.userIds.distinct.sorted(using stringOrdering) match
      case List(u1, u2) =>
        val result = Result(game.id, game.winnerUserId)
        val bsonResult = Crosstable.crosstableHandler.writeResult(result, u1)
        def incScore(userId: UserId): Int =
          game.winnerUserId match
            case Some(u) if u == userId => 10
            case None => 5
            case _ => 0
        val inc1 = incScore(u1)
        val inc2 = incScore(u2)
        val updateCrosstable = coll.update.one(
          select(u1, u2),
          $inc(
            F.score1 -> inc1,
            F.score2 -> inc2
          ) ++ $push(
            Crosstable.BSONFields.results -> $doc(
              "$each" -> List(bsonResult),
              "$slice" -> -Crosstable.maxGames
            )
          ),
          upsert = true
        )
        val updateMatchup = matchupColl:
          _.update
            .one(
              select(u1, u2),
              $inc(
                F.score1 -> inc1,
                F.score2 -> inc2
              ) ++ $set(
                F.lastPlayed -> nowInstant
              ),
              upsert = true
            )
            .void
        updateCrosstable.zip(updateMatchup).void
      case _ => funit

  private val matchupProjection = $doc(F.lastPlayed -> false)

  def getMatchup(u1: UserId, u2: UserId): Fu[Option[Matchup]] =
    matchupColl(_.find(select(u1, u2), matchupProjection.some).one[Matchup])

  private def select(u1: UserId, u2: UserId) =
    $id(Crosstable.makeKey(u1, u2))
