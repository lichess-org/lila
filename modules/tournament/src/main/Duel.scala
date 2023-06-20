package lila.tournament

import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.TreeSet

import lila.game.Game
import lila.user.User

case class Duel(
    gameId: GameId,
    p1: Duel.DuelPlayer,
    p2: Duel.DuelPlayer,
    averageRating: IntRating
):

  def has(u: User) = u.is(p1) || u.is(p2)

  def userIds = List[UserId](p1.name.id, p2.name.id)

object Duel:

  type UsernameRating = (UserName, IntRating)

  case class DuelPlayer(name: UserName, rating: IntRating, rank: Rank)
  object DuelPlayer:
    given UserIdOf[DuelPlayer] = _.name.id

  def tbUser(p: UsernameRating, ranking: Ranking) =
    ranking get p._1.id map { rank =>
      DuelPlayer(p._1, p._2, rank + 1)
    }

  private[tournament] val ratingOrdering              = Ordering.by[Duel, Int](_.averageRating.value)
  private[tournament] val gameIdOrdering              = Ordering.by[Duel, GameId](_.gameId)(stringOrdering)
  private[tournament] def emptyGameId(gameId: GameId) = Duel(gameId, null, null, IntRating(0))

final private class DuelStore:

  import Duel.*

  private val byTourId = new ConcurrentHashMap[TourId, TreeSet[Duel]](256)

  def get(tourId: TourId): Option[TreeSet[Duel]] = Option(byTourId get tourId)

  def bestRated(tourId: TourId, nb: Int): List[Duel] =
    get(tourId) so {
      lila.common.Heapsort.topNToList(_, nb)(using ratingOrdering)
    }

  def find(tour: Tournament, user: User): Option[GameId] =
    get(tour.id) flatMap { _.find(_ has user).map(_.gameId) }

  def add(tour: Tournament, game: Game, p1: UsernameRating, p2: UsernameRating, ranking: Ranking): Unit =
    for {
      p1 <- tbUser(p1, ranking)
      p2 <- tbUser(p2, ranking)
      tb = Duel(
        gameId = game.id,
        p1 = p1,
        p2 = p2,
        averageRating = IntRating((p1.rating.value + p2.rating.value) / 2)
      )
    } byTourId.compute(
      tour.id,
      (_: TourId, v: TreeSet[Duel]) => {
        if (v == null) TreeSet(tb)(gameIdOrdering)
        else v + tb
      }
    )

  def remove(game: Game): Unit =
    game.tournamentId foreach { tourId =>
      byTourId.computeIfPresent(
        tourId,
        (_: TourId, tb: TreeSet[Duel]) => {
          val w = tb - emptyGameId(game.id)
          if (w.isEmpty) null else w
        }
      )
    }
  def remove(tour: Tournament): Unit = byTourId.remove(tour.id).unit
