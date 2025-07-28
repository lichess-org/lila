package lila.tournament

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.id.TourPlayerId

import lila.core.chess.Rank

private type RankedPlayers = List[RankedPlayer]
private type Ranking = Map[UserId, Rank]
private type Waiting = Map[UserId, Rank]

private val logger = lila.log("tournament")
private val pairingLogger = logger.branch("pairing")
