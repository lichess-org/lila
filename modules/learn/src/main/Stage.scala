package lila.learn

sealed abstract class Stage(val id: Stage.Id)

object Stage {

  case class Id(value: String) extends AnyVal

  case object Intro extends Stage(Id("intro"))

  object pieces {
    case object Rook extends Stage(Id("pieces.rook"))
    case object Bishop extends Stage(Id("pieces.bishop"))
  }

  val all = List(
    Intro,
    pieces.Rook, pieces.Bishop
  )

  val idMap: Map[Id, Stage] = all.map { s => s.id -> s }.toMap

  def byId(id: Id): Option[Stage] = idMap get id
}
