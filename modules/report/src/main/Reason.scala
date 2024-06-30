package lila.report

import scalalib.Iso
import lila.core.user.Me

enum Reason:
  case Cheat
  case AltPrint
  case Comm
  case Boost
  case Username
  case Sexism
  case Other
  case Playbans
  def key    = toString.toLowerCase
  def name   = if this == AltPrint then "Print" else toString
  def isComm = this == Reason.Comm || this == Reason.Sexism

object Reason:
  val all       = values.toList
  val keys      = all.map(_.key)
  val byKey     = all.mapBy(_.key)
  val autoBlock = Set(Comm, Sexism)
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
      case Cheat                                  => Granter(_.MarkEngine)
      case Comm | Sexism                          => Granter(_.Shadowban)
      case Boost                                  => Granter(_.MarkBooster)
      case AltPrint | Playbans | Username | Other => Granter(_.Admin)
