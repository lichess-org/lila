package lila.report

private[report] sealed trait Reason {

  def name = toString.toLowerCase
}

private[report] object Reason {

  object Cheat extends Reason
  object Violence extends Reason
  object Troll extends Reason
  object Other extends Reason

  val all = List(Cheat, Violence, Troll, Other)
  val names = all map (_.name)
  val byName = all map { v ⇒ (v.name, v) } toMap

  def apply(name: String): Option[Reason] = byName get name
}
