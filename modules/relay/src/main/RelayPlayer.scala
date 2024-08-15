package lila.relay

import lila.core.fide.Federation

case class RelayPlayer(
    player: lila.study.StudyPlayer,
    ratingDiff: Option[Int],
    games: Vector[RelayPlayerCard.Game],
    fed: Option[Federation.Id],
    score: Double,
    played: Int
)
