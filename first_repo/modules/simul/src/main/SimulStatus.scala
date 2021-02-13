package lila.simul

sealed abstract private[simul] class SimulStatus(val id: Int) extends Ordered[SimulStatus] {

  def compare(other: SimulStatus) = Integer.compare(id, other.id)

  def name = toString

  def is(s: SimulStatus): Boolean                     = this == s
  def is(f: SimulStatus.type => SimulStatus): Boolean = is(f(SimulStatus))
}

private[simul] object SimulStatus {

  case object Created  extends SimulStatus(10)
  case object Started  extends SimulStatus(20)
  case object Finished extends SimulStatus(30)

  val all = List(Created, Started, Finished)

  val byId = all map { v =>
    (v.id, v)
  } toMap

  def apply(id: Int): Option[SimulStatus] = byId get id
}
