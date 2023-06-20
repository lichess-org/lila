package lila.tournament

private enum Status(val id: Int):

  case Created  extends Status(10)
  case Started  extends Status(20)
  case Finished extends Status(30)

  def name                                  = toString
  def is(s: Status): Boolean                = this == s
  def is(f: Status.type => Status): Boolean = is(f(Status))

object Status:
  val byId                           = values.mapBy(_.id)
  def apply(id: Int): Option[Status] = byId get id
