package lila.api

import lila.forum.Categ
import lila.security.{ Granter, Permission }
import lila.team.Team
import lila.user.{ User, UserContext }

final class ForumAccess(teamApi: lila.team.TeamApi, teamCached: lila.team.Cached)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  def isGrantedRead(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    Categ.slugToTeamId(categSlug).fold(fuTrue) { teamId =>
      ctx.me ?? { me =>
        fuccess(Granter(Permission.ModerateForum)(me)) >>| teamApi
          .belongsTo(teamId, me.id) >>& teamCached.forumAccess.get(teamId).flatMap {
          case Team.Access.NONE    => fuFalse
          case Team.Access.MEMBERS => fuTrue
          case Team.Access.LEADERS => teamApi.leads(teamId, me.id)
        }
      }
    }

  def isGrantedWrite(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    ctx.me.exists(canWriteInAnyForum) ?? isGrantedRead(categSlug)

  private def canWriteInAnyForum(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }

  def isGrantedMod(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.ModerateForum)) fuTrue
    else
      Categ.slugToTeamId(categSlug) ?? { teamId =>
        ctx.userId ?? {
          teamApi.leads(teamId, _)
        }
      }
}
