package lila.report

sealed trait Room {

  def key = toString.toLowerCase

  def name = toString
}

object Room {

  case object Cheat extends Room
  case object Print extends Room
  case object Coms extends Room
  case object Other extends Room
  case object Xfiles extends Room {
    override def name = "X-Files"
  }

  val all: List[Room] = List(Cheat, Print, Coms, Other, Xfiles)
  val byKey = all map { v => (v.key, v) } toMap

  implicit val roomIso = lila.common.Iso[String, Room](k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Room] = byKey get key

  def apply(reason: Reason) = reason match {
    case Reason.Cheat => Cheat
    case Reason.CheatPrint => Print
    case Reason.Troll | Reason.Insult => Coms
    case Reason.Boost | Reason.Other => Other
  }

  case class Counts(value: Map[Room, Int]) {
    def get = value.get _
    lazy val sum = value.filterKeys(Xfiles !=).values.sum
  }
}
