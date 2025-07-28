package lila.simul

private enum SimulStatus(val id: Int):

  case Created extends SimulStatus(10)
  case Started extends SimulStatus(20)
  case Finished extends SimulStatus(30)

  def name = toString

  def is(s: SimulStatus): Boolean = this == s
  def is(f: SimulStatus.type => SimulStatus): Boolean = is(f(SimulStatus))

private object SimulStatus:

  val byId = values.mapBy(_.id)

  def apply(id: Int): Option[SimulStatus] = byId.get(id)
