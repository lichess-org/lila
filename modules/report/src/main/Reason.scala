package lidraughts.report

sealed trait Reason {

  def key = toString.toLowerCase

  def name = toString
}

object Reason {

  case object Cheat extends Reason
  case object CheatPrint extends Reason {
    override def name = "Print"
  }
  case object Insult extends Reason
  case object Troll extends Reason
  case object CommFlag extends Reason
  case object Boost extends Reason
  case object Other extends Reason
  case object Playbans extends Reason

  val communication: Set[Reason] = Set(Insult, Troll, CommFlag, Other)

  val all = List(Cheat, CheatPrint, Insult, Troll, Boost, CommFlag, Other)
  val keys = all map (_.key)
  val byKey = all map { v => (v.key, v) } toMap

  implicit val reasonIso = lidraughts.common.Iso[String, Reason](k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Reason] = byKey get key

  trait WithReason {
    def reason: Reason

    def isCheat = reason == Cheat
    def isOther = reason == Other
    def isTroll = reason == Troll
    def isInsult = reason == Insult
    def isCommFlag = reason == CommFlag
    def isPrint = reason == CheatPrint
    def isAboutComm = isTroll || isInsult || isCommFlag
    def isPlaybans = reason == Playbans
  }
}
