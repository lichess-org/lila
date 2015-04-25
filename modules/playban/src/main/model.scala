package lila.playban

case class UserRecord(
    _id: String,
    h: List[Outcome]) {

  def userId = _id
  def history = h
}

sealed abstract class Outcome(
  val id: Int,
  val name: String)

object Outcome {

  case object Good extends Outcome(0, "Nothing unusual")
  case object Abort extends Outcome(1, "Aborts the game")
  case object NoPlay extends Outcome(2, "Won't play a move")

  val all = List(Good, Abort, NoPlay)

  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[Outcome] = byId get id
}
