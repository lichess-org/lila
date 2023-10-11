package lila
package team

import lila.user.{ Me, User, UserRepo }
import lila.security.Granter

final class TeamSecurity(teamRepo: TeamRepo, memberRepo: MemberRepo, userRepo: UserRepo)(using Executor):

  import TeamSecurity.*

  def setLeaders(team: Team, data: Seq[form.LeaderData])(by: Me): Funit =
    for
      allIds          <- memberRepo.filterUserIdsInTeam(team.id, data.map(_.name))
      idsNoKids       <- userRepo.filterNotKid(allIds.toSeq)
      previousLeaders <- memberRepo.leaders(team.id)
      byMod = Granter(_.ManageTeam)
      ids =
        if idsNoKids(User.lichessId) && !byMod && !previousValidLeaders(User.lichessId)
        then idsNoKids - User.lichessId
        else idsNoKids
      _ <- ids.nonEmpty.so:
        if ids(team.createdBy) || !previousValidLeaders(team.createdBy) || by.id == team.createdBy || byMod
        then
          cached.leaders.put(team.id, fuccess(ids))
          logger.info(s"valid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          teamRepo.setLeaders(team.id, ids).void
        else
          logger.info(s"invalid setLeaders ${team.id}: ${ids mkString ", "} by @${by.id}")
          funit
    yield ()

object TeamSecurity:

  object form:
    import play.api.data.*
    import play.api.data.Forms.*

    case class LeaderData(name: UserStr, perms: Set[TeamMember.Permission])

    private val leaderForm = mapping(
      "name" -> lila.user.UserForm.historicalUsernameField,
      "perms" -> seq(nonEmptyText)
        .transform[Set[TeamMember.Permission]](
          _.flatMap(TeamMember.Permission.byKey).toSet,
          _.toSeq.map(_.key)
        )
    )(LeaderData.apply)(unapply)

    def leaders(t: Team.WithLeaders): Form[Seq[LeaderData]] = Form(
      single("leaders" -> seq(leaderForm))
    ).fill(t.leaders.map(m => LeaderData(m.user into UserStr, m.perms)))
