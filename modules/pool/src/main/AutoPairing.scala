package lila.pool

import chess.Color

private[pool] object AutoPairing {

  private case class Sheet(score: Int, games: Int) {
    def add(by: Int) = copy(score = score + by, games = games + 1)
  }

  def apply(pool: Pool, userIds: Set[String]): (List[Pairing], List[Player]) = {

    val playingUserIds = pool.pairings.filter(_.playing).flatMap(_.users).toSet
    def inPoolRoom(p: Player) = userIds contains p.user.id
    def isPlaying(p: Player) = playingUserIds contains p.user.id
    val availablePlayers = pool.players filter inPoolRoom filterNot isPlaying
    val (pairablePlayers, unpairablePlayers) = availablePlayers partition (_.pairable)
    val nbPlaying = pool.players count isPlaying

    val minPlayersForPairing = math.min(6, (nbPlaying + unpairablePlayers.size) / 3)
    val nbPlayersForPairing = pairablePlayers.size
    val canDoPairings = nbPlayersForPairing >= minPlayersForPairing && nbPlayersForPairing % 2 == 0

    def makePairing(user1: String, user2: String) =
      pool moreRecentPairingOf user1 filter (_ contains user2) match {
        case Some(prev) => prev colorOf user1 match {
          case Some(Color.White) => Pairing(user2, user1)
          case _                 => Pairing(user1, user2)
        }
        case None => if (pool.whiteFrequencyOf(user1) > pool.whiteFrequencyOf(user2))
          Pairing(user2, user1) else Pairing(user1, user2)
      }

    val pairings = canDoPairings ?? {

      def pairs = pairablePlayers.map { player =>
        player -> pool.pairings.foldLeft(Sheet(0, 0)) {
          case (sheet, _) if sheet.games > 8                   => sheet
          case (sheet, pairing) if (pairing wonBy player.id)   => sheet add 1
          case (sheet, pairing) if (pairing drawnBy player.id) => sheet add 0
          case (sheet, pairing) if (pairing lostBy player.id)  => sheet add -1
          case (sheet, _)                                      => sheet
        }
      }.sortBy {
        case (player, sheet) => (-sheet.score, -player.rating)
      }.map(_._1) grouped 2

      pairs.map { scala.util.Random.shuffle(_) }.map {
        case List(p1, p2) => makePairing(p1.user.id, p2.user.id).some
        case _            => none
      }.toList.flatten
    }

    pairings -> pool.players.map {
      case p if isPlaying(p) => p
      case p if pairings.exists(_ contains p.user.id) => p setWaiting false
      case p if availablePlayers contains p => p setWaiting true
      case p => p
    }
  }
}
