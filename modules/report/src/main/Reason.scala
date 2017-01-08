package lila.report

sealed trait Reason {

  def key = toString.toLowerCase

  def name = toString
}

object Reason {

  case object Cheat extends Reason
  case object CheatPrint extends Reason {
    override def name = "Print"
  }
  case object Insult extends Reason
  case object Troll extends Reason
  case object Boost extends Reason
  case object Other extends Reason

  val communication: Set[Reason] = Set(Insult, Troll, Other)

  val all = List(Cheat, CheatPrint, Insult, Troll, Boost, Other)
  val keys = all map (_.key)
  val byKey = all map { v => (v.key, v) } toMap

  def apply(key: String): Option[Reason] = byKey get key
}
