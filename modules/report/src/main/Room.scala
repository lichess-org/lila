package lila.report

import cats.derived.*

import scalalib.Iso

enum Room derives Eq:

  case Cheat, Boost, Print, Comm, Other, Xfiles

  def key  = toString.toLowerCase
  def name = toString

object Room:

  val byKey = values.mapBy(_.key)

  val allButXfiles: List[Room] = values.filter(Xfiles != _).toList

  given Iso.StringIso[Room] = Iso.string(k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Room] = byKey.get(key)

  def apply(reason: Reason): Room =
    import lila.report.{ Reason as R }
    reason match
      case R.Cheat                           => Cheat
      case R.Boost                           => Boost
      case R.AltPrint | R.CheatPrint         => Print
      case R.Comm | R.Sexism                 => Comm
      case R.Other | R.Playbans | R.Username => Other

  case class Scores(value: Map[Room, Int]):
    def get     = value.get
    def highest = ~value.values.maxOption

  def isGranted(room: Room)(using Me) =
    import lila.core.perm.Granter
    room match
      case Cheat  => Granter[Me](_.MarkEngine)
      case Boost  => Granter[Me](_.MarkBooster)
      case Print  => Granter[Me](_.Admin)
      case Comm   => Granter[Me](_.Shadowban)
      case Other  => Granter[Me](_.Admin)
      case Xfiles => Granter[Me](_.MarkEngine)

  def filterGranted(reports: List[Report])(using mod: Me) = reports.filter: r =>
    isGranted(r.room) && (r.user.isnt(mod) || mod.isSuperAdmin)
