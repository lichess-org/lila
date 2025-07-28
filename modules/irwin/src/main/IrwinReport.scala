package lila.irwin

import lila.core.report.SuspectId

case class IrwinReport(
    _id: UserId,
    activation: Int, // 0 = clean, 100 = cheater
    games: List[IrwinReport.GameReport],
    owner: String, // thread sending the report, for monitoring
    date: Instant
):

  inline def userId = _id
  inline def suspectId = SuspectId(userId)

  def note: String = games
    .sortBy(-_.activation)
    .map { g =>
      s"#${g.gameId} = ${g.activation}"
    }
    .mkString(", ")

  def add(report: IrwinReport) =
    val newIds = report.games.map(_.gameId).toSet
    report.copy(
      games = games.filterNot(g => newIds(g.gameId)) ::: report.games
    )

object IrwinReport:

  case class GameReport(
      gameId: GameId,
      activation: Int,
      moves: List[MoveReport]
  )

  object GameReport:

    case class WithPov(report: GameReport, pov: Pov)

  case class MoveReport(
      activation: Int,
      rank: Option[Int], // selected PV, or null (if move is not in top 5)
      ambiguity: Int, // how many good moves are in the position
      odds: Int, // winning chances -100 -> 100
      loss: Int // percentage loss in winning chances
  ):
    override def toString =
      s"Rank: ${rank.fold("-")(_.toString)}, ambiguity: $ambiguity, odds: $odds, loss: $loss"

  case class WithPovs(report: IrwinReport, povs: Map[GameId, Pov]):
    def withPovs: List[GameReport.WithPov] = for
      gameReport <- report.games
      pov <- povs.get(gameReport.gameId)
    yield GameReport.WithPov(gameReport, pov)

  case class Dashboard(recent: List[IrwinReport]):

    def lastSeenAt = recent.headOption.map(_.date)

    def seenRecently = lastSeenAt.so(nowInstant.minusMinutes(15).isBefore)
