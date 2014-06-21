package lila.pool

private[pool] object AutoPairing {

  private case class Sheet(score: Int, games: Int) {
    def add(by: Int) = copy(score = score + by, games = games + 1)
    def average = score.toFloat / games
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

    val pairings = canDoPairings ?? {

      def basedOnRating = pairablePlayers.sortBy(-_.rating) grouped 2

      def basedOnWins = pairablePlayers.map { player =>
        player -> pool.pairings.foldLeft(Sheet(0, 0)) {
          case (sheet, _) if sheet.games > 8                   => sheet
          case (sheet, pairing) if (pairing wonBy player.id)   => sheet add 1
          case (sheet, pairing) if (pairing drawnBy player.id) => sheet add 0
          case (sheet, pairing) if (pairing lostBy player.id)  => sheet add -1
          case (sheet, _)                                      => sheet
        }
      }.sortBy {
        case (player, sheet) => (-sheet.average, -player.rating)
      }.map(_._1) grouped 2

      val pairs = basedOnWins

      pairs.map { scala.util.Random.shuffle(_) }.map {
        case List(p1, p2) => Pairing(p1.user.id, p2.user.id).some
        case _            => none
      }.toList.flatten
    }

    pairings -> pool.players.map {
      case p if isPlaying(p) => p
      case p if pairings.exists(_ contains p.user.id) => p.copy(pairable = false)
      case p if availablePlayers contains p => p.copy(pairable = true)
      case p => p
    }
  }
}
