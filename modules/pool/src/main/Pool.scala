package lila.pool

import chess.Color
import lila.common.LightUser
import lila.game.{ Game, PovRef }
import lila.user.User
import org.joda.time.{ DateTime, Seconds }

case class Pool(
    setup: PoolSetup,
    players: List[Player],
    pairings: List[Pairing],
    nextWaveAt: DateTime) {

  def secondsToNextWave = Seconds.secondsBetween(DateTime.now, nextWaveAt).getSeconds

  def sortedPlayers = players.sortBy(-_.rating)

  def rankedPlayersWith(me: Option[User]) =
    me.filterNot(contains).fold(sortedPlayers) { m =>
      (setup.playerOf(m) :: players).sortBy(-_.rating)
    }.zipWithIndex map {
      case (player, rank) => player -> (rank + 1)
    }

  lazy val nbPlayers = players.size

  def scoreOf(p: Player) = Player.Score(
    ratingPercent = 100 * (p.rating - minRating) / math.max(1, (maxRating - minRating)),
    recentPairings = pairings.foldLeft(List[Pairing]()) {
      case (res, pairing) if pairing.contains(p.user.id) && res.size <= 8 => pairing :: res
      case (res, _) => res
    })

  lazy val maxRating = if (players.isEmpty) 0 else players.map(_.rating).max
  lazy val minRating = if (players.isEmpty) 0 else players.map(_.rating).min

  def contains(userId: String): Boolean = players exists (_.user.id == userId)
  def contains(u: User): Boolean = contains(u.id)
  def contains(p: Player): Boolean = contains(p.user.id)

  def withPlayer(p: Player) = copy(players = p :: players).distinctPlayers
  def withUser(u: User) = withPlayer(setup playerOf u)

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

  lazy val playingPairings = pairings filter (_.playing)

  def isPlaying(userId: String) = playingPairings exists (_ contains userId)

  def userCurrentPov(userId: String): Option[PovRef] =
    playingPairings.flatMap(_ povRef userId).headOption

  def userCurrentPov(user: Option[User]): Option[PovRef] =
    user.flatMap(u => userCurrentPov(u.id))

  def userCurrentPairing(userId: String): Option[Pairing] =
    playingPairings find (_ contains userId)

  def playingPlayers = playingPairings.flatMap(_.users).toSet

  def nbWaitingPlayers = players count (_.waiting)

  def userCurrentPairingPov(userId: String): Option[(Pairing, PovRef)] =
    userCurrentPairing(userId) flatMap { pairing =>
      pairing.povRef(userId) map (pairing -> _)
    }

  def moreRecentPairingOf(userId: String): Option[Pairing] =
    pairings find (_ contains userId)

  def pairingsOf(userId: String): List[Pairing] = pairings filter (_ contains userId)

  def whiteFrequencyOf(userId: String): Float = {
    val ps = pairingsOf(userId)
    if (ps.isEmpty) 0.5f
    else ps.count(_.colorOf(userId) == Some(Color.White)).toFloat / ps.size
  }

  private def nbPairingsToSave = math.max(200, nbPlayers * 12)

  def withPairings(ps: List[Pairing]) =
    copy(pairings = (ps ::: pairings) take nbPairingsToSave)

  def finishGame(game: Game) = copy(
    pairings = pairings map {
      case p if p.gameId == game.id => p finish game
      case p                        => p
    }
  )
}
