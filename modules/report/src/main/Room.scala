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

  def apply(reason: Reason): Room = reason match {
    case Reason.Cheat => Cheat
    case Reason.CheatPrint => Print
    case Reason.Troll | Reason.Insult => Coms
    case Reason.Boost | Reason.Playbans | Reason.Other => Other
  }

  def toReasons(room: Room): Set[Reason] = room match {
    case Cheat => Set(Reason.Cheat)
    case Print => Set(Reason.CheatPrint)
    case Coms => Set(Reason.Troll, Reason.Insult)
    case Other => Set(Reason.Boost, Reason.Other)
    case Xfiles => Set.empty
  }

  case class Counts(value: Map[Room, Int]) {
    def get = value.get _
    lazy val sum = value.filterKeys(Xfiles !=).values.sum
  }
}
