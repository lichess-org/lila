package lila.pool

private[pool] object AutoPairing {

  def apply(pool: Pool, userIds: Set[String]): List[Pairing] = {
    val playingUserIds = pool.pairings.filter(_.playing).flatMap(_.users).toSet
    def inPoolRoom(p: Player) = userIds contains p.user.id
    def isPlaying(p: Player) = playingUserIds contains p.user.id
    val availablePlayers = pool.players filter inPoolRoom filterNot isPlaying
    (availablePlayers.sortBy(-_.rating) grouped 2).map { scala.util.Random.shuffle(_) }.map {
      case List(p1, p2) => Pairing(p1.user.id, p2.user.id).some
      case _            => none
    }.toList.flatten
  }
}
