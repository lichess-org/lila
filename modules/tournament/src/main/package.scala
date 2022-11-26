package lila.tournament

export lila.Lila.{ *, given }

private type RankedPlayers = List[RankedPlayer]
private type Ranking       = Map[lila.user.User.ID, Rank]
private type Waiting       = Map[lila.user.User.ID, Rank]

private val logger        = lila.log("tournament")
private val pairingLogger = logger branch "pairing"
