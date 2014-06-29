package lila.pool

import chess.Color

private[pool] object AutoPairing {

  private case class Sheet(score: Int, games: Int) {
    def add(by: Int) = copy(score = score + by, games = games + 1)
  }

  def apply(pool: Pool, userIds: Set[String]): List[Pairing] = {

    val playingUserIds = pool.playingPlayers
    val pairablePlayers = pool.players filter { p =>
      p.pairable && userIds(p.user.id) && !playingUserIds(p.user.id)
    }

    def makePairing(user1: String, user2: String) =
      pool moreRecentPairingOf user1 filter (_ contains user2) match {
        case Some(prev) => prev colorOf user1 match {
          case Some(Color.White) => Pairing(user2, user1)
          case _                 => Pairing(user1, user2)
        }
        case None =>
          val (f1, f2) = pool.whiteFrequencyOf(user1) -> pool.whiteFrequencyOf(user2)
          if (f1 > f2) Pairing(user2, user1)
          else if (f2 > f1) Pairing(user1, user2)
          else if (scala.util.Random.nextBoolean) Pairing(user2, user1)
          else Pairing(user1, user2)
      }

    def pairs = dropExtraPlayer(pairablePlayers).map { player =>
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

    pairs.map {
      case List(p1, p2) => makePairing(p1.user.id, p2.user.id).some
      case _            => none
    }.toList.flatten
  }

  def dropExtraPlayer(players: List[Player]) =
    if (players.size % 2 == 0) players else {
      players.sortBy { p =>
        - p.waitingSince.fold(0L)(_.getSeconds)
      } drop 1
    }
}
