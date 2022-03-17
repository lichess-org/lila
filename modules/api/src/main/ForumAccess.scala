package lila.api

import lila.forum.Categ
import lila.security.{ Granter, Permission }
import lila.team.Team
import lila.user.{ User, UserContext }

final class ForumAccess(teamApi: lila.team.TeamApi) {

  private def userBelongsToTeam(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    teamApi.belongsTo(teamId, userId)

  private def userOwnsTeam(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    teamApi.leads(teamId, userId)

  def isGrantedRead(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    ctx.me ?? { me =>
      Categ.slugToTeamId(categSlug).fold(fuTrue) { teamId =>
        userBelongsToTeam(teamId, me.id)
      }
    }

  def isGrantedWrite(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    ctx.me.filter(canForum) ?? { me =>
      Categ.slugToTeamId(categSlug).fold(fuTrue) { teamId =>
        userBelongsToTeam(teamId, me.id)
      }
    }

  private def canForum(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }

  def isGrantedMod(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.ModerateForum)) fuTrue
    else
      Categ.slugToTeamId(categSlug) ?? { teamId =>
        ctx.userId ?? {
          userOwnsTeam(teamId, _)
        }
      }
}
