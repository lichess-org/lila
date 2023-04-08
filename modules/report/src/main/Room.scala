package lila.report

import lila.user.Holder
import lila.common.Iso

enum Room:

  case Cheat, Boost, Print, Comm, Other, Xfiles

  def key  = toString.toLowerCase
  def name = toString

object Room:

  val byKey = values.mapBy(_.key)

  val allButXfiles: List[Room] = values.filter(Xfiles != _).toList

  given Iso.StringIso[Room] = Iso.string(k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Room] = byKey get key

  def apply(reason: Reason): Room =
    reason match
      case Reason.Cheat                        => Cheat
      case Reason.Boost                        => Boost
      case Reason.AltPrint | Reason.CheatPrint => Print
      case Reason.Comm                         => Comm
      case Reason.Other | Reason.Playbans      => Other

  def toReasons(room: Room): Set[Reason] =
    room match
      case Cheat  => Set(Reason.Cheat)
      case Boost  => Set(Reason.Boost)
      case Print  => Set(Reason.AltPrint)
      case Comm   => Set(Reason.Comm)
      case Other  => Set(Reason.Playbans, Reason.Other)
      case Xfiles => Set.empty

  case class Scores(value: Map[Room, Int]):
    def get     = value.get
    def highest = ~value.values.maxOption

  def isGrantedFor(mod: Holder)(room: Room) =
    import lila.security.Granter
    room match
      case Cheat  => Granter.is(_.MarkEngine)(mod)
      case Boost  => Granter.is(_.MarkBooster)(mod)
      case Print  => Granter.is(_.Admin)(mod)
      case Comm   => Granter.is(_.Shadowban)(mod)
      case Other  => Granter.is(_.Admin)(mod)
      case Xfiles => Granter.is(_.MarkEngine)(mod)

  def filterGranted(mod: Holder, reports: List[Report]) = reports.filter { r =>
    isGrantedFor(mod)(r.room) && (r.user != mod.id || mod.user.isSuperAdmin)
  }
