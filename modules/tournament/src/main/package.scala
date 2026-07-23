package lila.tournament

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.id.TourPlayerId
export lila.gathering.Payouts

import lila.core.chess.Rank

private type RankedPlayers = List[RankedPlayer]
private type Ranking = Map[UserId, Rank]
private type Waiting = Map[UserId, Rank]

private lazy val logger = lila.log("tournament")
