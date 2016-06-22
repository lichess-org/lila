package lila.learn

sealed abstract class Stage(val id: Stage.Id)

object Stage {

  case class Id(value: String) extends AnyVal

  case object Intro extends Stage(Id("intro"))

  val all = List(Intro)

  def byId(id: Id) = all.find(_.id == id)
}
