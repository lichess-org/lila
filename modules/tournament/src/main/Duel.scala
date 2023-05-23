package lila.tournament

import lila.game.Game
import lila.user.User

import java.util.concurrent.ConcurrentHashMap

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
}

final private class DuelStore {

  import Duel._

  private val byTourId = new ConcurrentHashMap[Tournament.ID, Vector[Duel]](256)

  def get(tourId: Tournament.ID): Option[Vector[Duel]] = Option(byTourId get tourId)

  def bestRated(tourId: Tournament.ID, nb: Int): Vector[Duel] =
    ~get(tourId) sortBy (-_.averageRating.value) take nb

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
    } byTourId.put(tour.id, get(tour.id).fold(Vector(tb)) { _ :+ tb })

  def remove(game: Game): Unit =
    for {
      tourId <- game.tournamentId
      tb     <- get(tourId)
    } {
      if (tb.sizeIs <= 1) byTourId.remove(tourId)
      else byTourId.put(tourId, tb.filter(_.gameId != game.id))
    }

  def remove(tour: Tournament): Unit = byTourId.remove(tour.id).unit
}
