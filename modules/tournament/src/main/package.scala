package lila.tournament

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.id.TourPlayerId
export lila.gathering.Payouts

import lila.core.chess.Rank
import lila.core.user.{ RealName, LightUserApi }

private type RankedPlayers = List[RankedPlayer]
private type Ranking = Map[UserId, Rank]
private type Waiting = Map[UserId, Rank]

private lazy val logger = lila.log("tournament")

private type GetRealName = UserId => Option[RealName]
private def getRealName(tour: Tournament)(using lightUserApi: LightUserApi): GetRealName =
  if tour.realNames then lightUserApi.realName else _ => none
