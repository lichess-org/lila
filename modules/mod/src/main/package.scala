package lila.mod

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.userId.ModId
import lila.core.perf.UserWithPerfs
import lila.core.perm.{ Granter, Permission }
import lila.core.misc.AtInstant

private val logger = lila.log("mod")

final class ModlogRepo(val coll: lila.db.dsl.Coll)
final class AssessmentRepo(val coll: lila.db.dsl.Coll)
final class HistoryRepo(val coll: lila.db.dsl.Coll)
final class ModQueueStatsRepo(val coll: lila.db.dsl.Coll)

case class UserWithModlog(user: UserWithPerfs, log: List[Modlog.UserEntry]):
  export user.user.*
  def dateOf(action: Modlog.type => String): Option[Instant] =
    log.find(_.action == action(Modlog)).map(_.date)

object UserWithModlog:
  given UserIdOf[UserWithModlog] = _.user.id
  given AtInstant[UserWithModlog] = _.user.createdAt

def canGrant(permission: Permission)(using Me): Boolean =
  Granter(_.SuperAdmin) || {
    Granter(_.ChangePermission) && Permission.nonModPermissions(permission)
  } || {
    Granter(_.Admin) && {
      Granter(permission) || Set[Permission](
        Permission.MonitoredCheatMod,
        Permission.MonitoredBoostMod,
        Permission.MonitoredCommMod,
        Permission.PublicMod
      )(permission)
    }
  }

def canCloseAlt(using Me) = Granter(_.CloseAccount) && Granter(_.ViewPrintNoIP)

def canViewAltUsername(user: User)(using Option[Me]): Boolean =
  Granter.opt(_.Admin) || {
    (Granter.opt(_.CheatHunter) && user.marks.engine) ||
    (Granter.opt(_.BoostHunter) && user.marks.boost) ||
    (Granter.opt(_.Shusher) && user.marks.troll)
  }
