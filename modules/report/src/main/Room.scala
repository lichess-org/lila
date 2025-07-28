package lila.report

import cats.derived.*
import scalalib.Iso

enum Room derives Eq:

  case Cheat, Boost, Print, Comm, Other, Xfiles

  def key = toString.toLowerCase
  def name = toString

object Room:

  val byKey = values.mapBy(_.key)

  val allButXfiles: List[Room] = values.filter(Xfiles != _).toList

  given Iso.StringIso[Room] = Iso.string(k => byKey.getOrElse(k, Other), _.key)

  def apply(key: String): Option[Room] = byKey.get(key)

  def apply(reason: Reason): Room =
    import lila.report.Reason as R
    reason match
      case R.Cheat => Cheat
      case R.Boost => Boost
      case R.AltPrint => Print
      case r if r.isComm => Comm
      case _ => Other

  case class Scores(value: Map[Room, Int]):
    def get = value.get
    def highest = ~value.values.maxOption

  def isGranted(room: Room)(using Me) =
    import lila.core.perm.Granter
    room match
      case Cheat => Granter(_.MarkEngine)
      case Boost => Granter(_.MarkBooster)
      case Print => Granter(_.Admin)
      case Comm => Granter(_.Shadowban)
      case Other => Granter(_.Admin)
      case Xfiles => Granter(_.MarkEngine)

  def filterGranted(reports: List[Report])(using mod: Me) = reports.filter: r =>
    isGranted(r.room) && (r.user.isnt(mod) || mod.isSuperAdmin)

  import lila.core.irc.ModDomain
  import lila.core.perm.Granter
  def ircDomain(room: Room)(using Me): ModDomain = room match
    case Room.Cheat => ModDomain.Cheat
    case Room.Boost => ModDomain.Boost
    case Room.Comm => ModDomain.Comm
    // spontaneous inquiry
    case _ if Granter(_.Admin) => ModDomain.Admin
    case _ if Granter(_.CheatHunter) => ModDomain.Cheat // heuristic
    case _ if Granter(_.Shusher) => ModDomain.Comm
    case _ if Granter(_.BoostHunter) => ModDomain.Boost
    case _ => ModDomain.Admin
