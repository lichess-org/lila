package lila.swiss

sealed abstract private[swiss] class Status(val id: Int) extends Ordered[Status] {
  def compare(other: Status) = Integer.compare(id, other.id)
  def name                   = toString
  def is(s: Status): Boolean = this == s
}

private[swiss] object Status {

  case object Created  extends Status(10)
  case object Started  extends Status(20)
  case object Finished extends Status(30)

  val all = List(Created, Started, Finished)

  val byId = all map { v =>
    (v.id, v)
  } toMap

  def apply(id: Int): Option[Status] = byId get id
}
