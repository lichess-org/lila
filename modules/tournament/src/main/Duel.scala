package lila.tournament

import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.TreeSet

import lila.game.Game
import lila.user.User

case class Duel(
    gameId: Game.ID,
    p1: Duel.DuelPlayer,
    p2: Duel.DuelPlayer,
    averageRating: Duel.Rating
) {

  def has(u: User) = p1.name.id == u.id || p2.name.id == u.id

  def userIds = List(p1.name.id, p2.name.id)
}

object Duel {

  type UsernameRating = (String, Int)

  case class DuelPlayer(name: Name, rating: Rating, rank: Rank)
  case class Name(value: String) extends AnyVal with StringValue {
    def id = User normalize value
  }
  case class Rating(value: Int) extends AnyVal with IntValue
  case class Rank(value: Int)   extends AnyVal with IntValue

  def tbUser(p: UsernameRating, ranking: Ranking) =
    ranking get User.normalize(p._1) map { rank =>
      DuelPlayer(Name(p._1), Rating(p._2), Rank(rank + 1))
    }

  private[tournament] val ratingOrdering               = Ordering.by[Duel, Int](_.averageRating.value)
  private[tournament] val gameIdOrdering               = Ordering.by[Duel, Game.ID](_.gameId)
  private[tournament] def emptyGameId(gameId: Game.ID) = Duel(gameId, null, null, Rating(0))
}

final private class DuelStore {

  import Duel._

  private val byTourId = new ConcurrentHashMap[Tournament.ID, TreeSet[Duel]](256)

  def get(tourId: Tournament.ID): Option[TreeSet[Duel]] = Option(byTourId get tourId)

  def bestRated(tourId: Tournament.ID, nb: Int): List[Duel] =
    get(tourId) ?? {
      lila.common.Heapsort.topNToList(_, nb, ratingOrdering)
    }

  def find(tour: Tournament, user: User): Option[Game.ID] =
    get(tour.id) flatMap { _.find(_ has user).map(_.gameId) }

  def add(tour: Tournament, game: Game, p1: UsernameRating, p2: UsernameRating, ranking: Ranking): Unit =
    for {
      p1 <- tbUser(p1, ranking)
      p2 <- tbUser(p2, ranking)
      tb = Duel(
        gameId = game.id,
        p1 = p1,
        p2 = p2,
        averageRating = Rating((p1.rating.value + p2.rating.value) / 2)
      )
    } byTourId.compute(
      tour.id,
      (_: Tournament.ID, v: TreeSet[Duel]) => {
        if (v == null) TreeSet(tb)(gameIdOrdering)
        else v + tb
      }
    )

  def remove(game: Game): Unit =
    game.tournamentId foreach { tourId =>
      byTourId.computeIfPresent(
        tourId,
        (_: Tournament.ID, tb: TreeSet[Duel]) => {
          val w = tb - emptyGameId(game.id)
          if (w.isEmpty) null else w
        }
      )
    }
  def remove(tour: Tournament): Unit = byTourId.remove(tour.id)
}
