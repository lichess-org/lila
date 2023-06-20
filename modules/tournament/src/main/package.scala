package lila.tournament

export lila.Lila.{ *, given }

private type RankedPlayers = List[RankedPlayer]
private type Ranking       = Map[UserId, Rank]
private type Waiting       = Map[UserId, Rank]

private val logger        = lila.log("tournament")
private val pairingLogger = logger branch "pairing"
