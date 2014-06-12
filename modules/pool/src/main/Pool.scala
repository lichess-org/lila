package lila.pool

import lila.common.LightUser
import lila.game.Game
import lila.user.User

case class Pool(
    setup: PoolSetup,
    players: List[Player],
    pairings: List[Pairing]) {

  lazy val sortedPlayers = players.sortBy(-_.rating)

  lazy val rankedPlayers = sortedPlayers.zipWithIndex map {
    case (player, rank) => player -> (rank + 1)
  }

  lazy val nbPlayers = players.size

  def contains(userId: String): Boolean = players exists (_.user.id == userId)
  def contains(u: User): Boolean = contains(u.id)
  def contains(p: Player): Boolean = contains(p.user.id)

  def withPlayer(p: Player) = copy(players = p :: players).distinctPlayers
  def withUser(u: User) = withPlayer(Player(
    LightUser(u.id, u.username, u.title),
    setup.glickoLens(u).intRating
  ))

  def updatePlayers(users: List[User]) = copy(
    players = players map { player =>
      users find (_.id == player.id) match {
        case Some(user) => player withRating setup.glickoLens(user).intRating
        case None       => player
      }
    }
  )

  def filterPlayers(cond: Player => Boolean) = copy(players = players filter cond)

  private def distinctPlayers = copy(
    players = players.map { p =>
      p.user.id -> p
    }.toMap.values.toList
  )

  def withoutPlayer(p: Player) = copy(players = players filterNot (_ is p))
  def withoutUserId(id: String) = copy(players = players filterNot (_.user.id == id))

  def playersAround(uo: Option[User], nb: Int) = uo.fold(sortedPlayers take nb) { u =>
    val rating = setup.glickoLens(u).intRating
    players.sortBy(p => math.abs(rating - p.rating)) take nb sortBy (-_.rating)
  }

  private def nbPairingsToSave = math.max(100, nbPlayers * 5)

  def withPairings(ps: List[Pairing]) =
    copy(pairings = (ps ::: pairings) take nbPairingsToSave)

  def finishGame(game: Game) = copy(
    pairings = pairings map {
      case p if p.gameId == game.id => p.finish(game.status, game.turns, game.winnerUserId)
      case p                        => p
    }
  )
}
