package lila.report

import scalalib.Iso

import lila.core.user.Me

enum Reason:
  case Cheat
  case Stall
  case Boost
  case Comm   // BC
  case Sexism // BC
  case VerbalAbuse
  case Violence
  case Harass
  case SelfHarm
  case Hate
  case Spam
  case Username
  case Other
  // auto reports:
  case Playbans
  case AltPrint
  def key    = toString.toLowerCase
  def name   = if this == AltPrint then "Print" else toString
  def isComm = Reason.comm(this)

object Reason:
  val all       = values.toList
  val keys      = all.map(_.key)
  val byKey     = all.mapBy(_.key)
  val comm      = Set(Comm, Sexism, VerbalAbuse, Violence, Harass, SelfHarm, Hate, Spam)
  val autoBlock = comm
  val flagText  = "[FLAG]"

  given Iso.StringIso[Reason] = Iso.string(k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Reason] = byKey.get(key)

  trait WithReason:
    def reason: Reason

    def isComm                            = reason.isComm
    def isCheat                           = reason == Cheat
    def isOther                           = reason == Other
    def isPrint                           = reason == AltPrint
    def isBoost                           = reason == Boost
    def is(reason: Reason.type => Reason) = this.reason == reason(Reason)

  def isGranted(reason: Reason)(using Me) =
    import lila.core.perm.Granter
    reason match
      case Cheat         => Granter(_.MarkEngine)
      case r if r.isComm => Granter(_.Shadowban)
      case Boost         => Granter(_.MarkBooster)
      case _             => Granter(_.Admin)
