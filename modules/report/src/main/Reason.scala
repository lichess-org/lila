package lila.report

sealed trait Reason {

  def name = toString.toLowerCase
}

object Reason {

  case object Cheat extends Reason
  case object Violence extends Reason
  case object Troll extends Reason
  case object Other extends Reason

  val all = List(Cheat, Violence, Troll, Other)
  val names = all map (_.name)
  val byName = all map { v ⇒ (v.name, v) } toMap

  def apply(name: String): Option[Reason] = byName get name
}
