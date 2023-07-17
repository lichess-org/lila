package lila.mod

import lila.user.User

export lila.Lila.{ *, given }

private val logger = lila.log("mod")

final class ModlogRepo(val coll: lila.db.dsl.Coll)
final class AssessmentRepo(val coll: lila.db.dsl.Coll)
final class HistoryRepo(val coll: lila.db.dsl.Coll)
final class ModQueueStatsRepo(val coll: lila.db.dsl.Coll)

case class UserWithModlog(user: User.WithPerfs, log: List[Modlog.UserEntry]):
  export user.user.*
  def dateOf(action: Modlog.type => String): Option[Instant] =
    log.find(_.action == action(Modlog)).map(_.date)

object UserWithModlog:
  given UserIdOf[UserWithModlog] = _.user.id
