package lila.report

import lila.user.User

sealed trait Reason {

  def key = toString.toLowerCase

  def name = toString
}

object Reason {

  case object Cheat extends Reason
  case object CheatPrint extends Reason {
    override def name = "Print"
  }
  case object Comm extends Reason {
    def flagText = "[FLAG]"
  }
  case object Boost    extends Reason
  case object Other    extends Reason
  case object Playbans extends Reason

  // val communication: Set[Reason] = Set(Insult, Troll, CommFlag, Other)

  val all  = List(Cheat, CheatPrint, Comm, Boost, Other)
  val keys = all map (_.key)
  val byKey = all map { v =>
    (v.key, v)
  } toMap

  implicit val reasonIso = lila.common.Iso[String, Reason](k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Reason] = byKey get key

  trait WithReason {
    def reason: Reason

    def isCheat    = reason == Cheat
    def isOther    = reason == Other
    def isPrint    = reason == CheatPrint
    def isComm     = reason == Comm
    def isPlaybans = reason == Playbans
  }

  def isGrantedFor(mod: User)(reason: Reason) = {
    import lila.security.Granter
    reason match {
      case Cheat                    => Granter(_.MarkEngine)(mod)
      case CheatPrint               => Granter(_.ViewIpPrint)(mod)
      case Comm                     => Granter(_.Shadowban)(mod)
      case Boost | Playbans | Other => Granter(_.MarkBooster)(mod)
    }
  }
}
