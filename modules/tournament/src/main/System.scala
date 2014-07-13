package lila.tournament

import scala.concurrent.Future

sealed abstract class System(val id: Int) {
  val pairingSystem: PairingSystem
  val scoringSystem: ScoringSystem
}

object System {
  case object Arena extends System(id = 1) {
    val pairingSystem = arena.PairingSystem
    val scoringSystem = arena.ScoringSystem
  }

  case object Swiss extends System(id = 2) {
    val pairingSystem = swiss.SwissSystem
    val scoringSystem = swiss.SwissSystem
  }

  val default = Arena

  val all = List(Arena, Swiss)

  val byId = all map { s => (s.id -> s) } toMap

  def apply(id: Int): Option[System] = byId get id
  def orDefault(id: Int): System = apply(id) getOrElse default
}

trait PairingSystem {
  def createPairings(tournament: Tournament, users: List[String]): Future[(Pairings,Events)]
}

trait Score {
  val value: Int
}

trait ScoreSheet {
  def scores: List[Score]
  def total:  Int
}

trait ScoringSystem {
  type Sheet <: ScoreSheet
  type RankedPlayers = List[(Int, Player)]

  // This should rank players by score, and rank all withdrawn players after active ones.
  def rank(tournament: Tournament, players: Players): RankedPlayers

  // You must override either this one or scoreSheet!
  def scoreSheets(tournament: Tournament): Map[String,Sheet] = {
    tournament.players.map { p =>
      (p.id -> scoreSheet(tournament, p.id))
    } toMap
  }

  // You must override either this one or scoreSheets!
  def scoreSheet(tournament: Tournament, player: String): Sheet = scoreSheets(tournament)(player)
}
