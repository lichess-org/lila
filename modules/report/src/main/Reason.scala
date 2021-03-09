package lila.report

import lila.user.{ Holder, User }

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
    def isBoost    = reason == Boost
    def isPlaybans = reason == Playbans
  }

  def isGrantedFor(mod: Holder)(reason: Reason) = {
    import lila.security.Granter
    reason match {
      case Cheat                    => Granter.is(_.MarkEngine)(mod)
      case CheatPrint               => Granter.is(_.Admin)(mod)
      case Comm                     => Granter.is(_.Shadowban)(mod)
      case Boost | Playbans | Other => Granter.is(_.MarkBooster)(mod)
    }
  }
}
