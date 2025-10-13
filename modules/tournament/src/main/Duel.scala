package lila.tournament

import scala.collection.immutable.TreeSet
import chess.IntRating

import lila.core.chess.Rank

case class Duel(
    gameId: GameId,
    p1: Duel.DuelPlayer,
    p2: Duel.DuelPlayer,
    averageRating: IntRating
):

  def has(u: UserId) = u.is(p1) || u.is(p2)

  def userIds = List[UserId](p1.name.id, p2.name.id)

object Duel:

  type UsernameRating = (user: UserName, rating: IntRating)

  case class DuelPlayer(name: UserName, rating: IntRating, rank: Rank)
  object DuelPlayer:
    given UserIdOf[DuelPlayer] = _.name.id

  def tbUser(p: UsernameRating, ranking: Ranking) =
    ranking.get(p.user.id).map { rank =>
      DuelPlayer(p.user, p.rating, rank + 1)
    }

  private[tournament] val ratingOrdering = Ordering.by[Duel, Int](_.averageRating.value)
  private[tournament] val gameIdOrdering = Ordering.by[Duel, GameId](_.gameId)(using stringOrdering)

final private class DuelStore:

  import Duel.*

  private val byTourId = scalalib.ConcurrentMap[TourId, TreeSet[Duel]](256)

  export byTourId.get

  def bestRated(tourId: TourId, nb: Int): List[Duel] =
    get(tourId).so:
      scalalib.HeapSort.topNToList(_, nb)(using ratingOrdering)

  def find(tour: Tournament, user: UserId): Option[GameId] =
    get(tour.id).flatMap { _.find(_.has(user)).map(_.gameId) }

  def add(tour: TourId, game: GameId, p1: UsernameRating, p2: UsernameRating, ranking: Ranking): Unit =
    makeDuel(game, p1, p2, ranking).foreach(add(tour, _))

  def add(tour: TourId, duel: Duel): Unit =
    byTourId.compute(tour):
      _.fold(TreeSet(duel)(using gameIdOrdering))(_ + duel).some

  def makeDuel(game: GameId, p1: UsernameRating, p2: UsernameRating, ranking: Ranking): Option[Duel] = for
    p1 <- tbUser(p1, ranking)
    p2 <- tbUser(p2, ranking)
  yield Duel(
    gameId = game,
    p1 = p1,
    p2 = p2,
    averageRating = (p1.rating + p2.rating).map(_ / 2)
  )

  def remove(game: Game): Unit =
    game.tournamentId.foreach(remove(game.id, _))

  def remove(game: GameId, tour: TourId): Unit =
    byTourId.computeIfPresent(tour): tb =>
      // only Duel.gameId is used for set equality
      // so we use a volatile duel with null players :-/
      val w = tb - Duel(game, null, null, IntRating(0))
      Option.when(w.nonEmpty)(w)

  def remove(tour: Tournament): Unit = byTourId.remove(tour.id)

  def kick(tour: Tournament, user: UserId): Unit =
    find(tour, user).foreach(remove(_, tour.id))
