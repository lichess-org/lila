package lila.tournament

sealed abstract class System(val id: Int) {
  val pairingSystem: PairingSystem
  val scoringSystem: ScoringSystem
  val berserkable: Boolean

  def default = this == System.Arena
}

object System {
  case object Arena extends System(id = 1) {
    val pairingSystem = arena.PairingSystem
    val scoringSystem = arena.ScoringSystem
    val berserkable = true
  }

  val default = Arena

  val all = List(Arena)

  val byId = all map { s => (s.id -> s) } toMap

  def apply(id: Int): Option[System] = byId get id
  def orDefault(id: Int): System = apply(id) getOrElse default
}

trait PairingSystem {
  def createPairings(
    tournament: Tournament,
    users: WaitingUsers,
    ranking: Ranking
  ): Fu[Pairings]
}

trait Score {
  val value: Int
}

trait ScoreSheet {
  def scores: List[Score]
  def total: Int
  def onFire: Boolean
}

trait ScoringSystem {
  type Sheet <: ScoreSheet

  def emptySheet: Sheet

  def sheet(userId: String, pairings: Pairings): Sheet
}
