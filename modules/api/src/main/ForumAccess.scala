package lila.api

import lila.forum.Categ
import lila.security.{ Granter, Permission }
import lila.team.Team
import lila.user.{ User, UserContext }

final class ForumAccess(teamApi: lila.team.TeamApi, teamCached: lila.team.Cached)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  sealed trait Operation
  case object Read  extends Operation
  case object Write extends Operation

  private def isGranted(categSlug: String, op: Operation)(implicit ctx: UserContext): Fu[Boolean] =
    Categ.slugToTeamId(categSlug).fold(fuTrue) { teamId =>
      ctx.me ?? { me =>
        teamCached.forumAccess.get(teamId).flatMap {
          case Team.Access.NONE => fuFalse
          case Team.Access.EVERYONE =>
            op match {
              case Read => fuTrue
              case Write =>
                teamApi.belongsTo(
                  teamId,
                  me.id
                ) // when the team forum is open to everyone, you still need to belong to the team in order to post
            }
          case Team.Access.MEMBERS => teamApi.belongsTo(teamId, me.id)
          case Team.Access.LEADERS => teamApi.leads(teamId, me.id)
        }
      }
    }

  def isGrantedRead(categSlug: String)(implicit ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.Shusher)) fuTrue
    else isGranted(categSlug, Read)

  def isGrantedWrite(categSlug: String, tryingToPostAsMod: Boolean = false)(implicit
      ctx: UserContext
  ): Fu[Boolean] =
    if (tryingToPostAsMod && ctx.me ?? Granter(Permission.Shusher)) fuTrue
    else ctx.me.exists(canWriteInAnyForum) ?? isGranted(categSlug, Write)

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
