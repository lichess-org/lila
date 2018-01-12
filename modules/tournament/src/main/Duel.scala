package lila.tournament

import lila.game.Game
import lila.user.User

import scala.collection.mutable.AnyRefMap

case class Duel(
    gameId: Game.ID,
    p1: Duel.DuelPlayer,
    p2: Duel.DuelPlayer,
    averageRating: Duel.Rating
)

object Duel {

  case class DuelPlayer(name: Name, rating: Rating, rank: Rank)
  case class Name(value: String) extends AnyVal with StringValue {
    def id = User normalize value
  }
  case class Rating(value: Int) extends AnyVal with IntValue
  case class Rank(value: Int) extends AnyVal with IntValue

  def tbUser(p: Player.WithUser, ranking: Ranking) = ranking get p.user.id map { rank =>
    DuelPlayer(Name(p.user.username), Rating(p.player.rating), Rank(rank))
  }
}

final class DuelStore {

  import Duel._

  private val byTourId = AnyRefMap.empty[Tournament.ID, Vector[Duel]]

  def bestRated(tourId: Tournament.ID, nb: Int): Vector[Duel] =
    ~(byTourId get tourId) sortBy (-_.averageRating.value) take nb

  def count = byTourId.pp.size

  def add(tour: Tournament, game: Game, p1: Player.WithUser, p2: Player.WithUser, ranking: Ranking): Unit = for {
    p1 <- tbUser(p1, ranking)
    p2 <- tbUser(p2, ranking)
    tb = Duel(
      gameId = game.id,
      p1 = p1,
      p2 = p2,
      averageRating = Rating((p1.rating.value + p2.rating.value) / 2)
    )
  } byTourId += (tour.id -> byTourId.get(tour.id).fold(Vector(tb)) { _ :+ tb })

  def remove(game: Game): Unit = for {
    tourId <- game.tournamentId
    tb <- byTourId get tourId
  } {
    if (tb.size <= 1) byTourId -= tourId
    else byTourId += (tourId, tb.filter(_.gameId != game.id))
  }
}
