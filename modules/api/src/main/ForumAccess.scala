package lila.api

import lila.forum.ForumCateg
import lila.security.{ Granter, Permission }
import lila.team.Team
import lila.user.{ User, UserContext }

final class ForumAccess(teamApi: lila.team.TeamApi, teamCached: lila.team.Cached)(using
    ec: scala.concurrent.ExecutionContext
):

  sealed trait Operation
  case object Read  extends Operation
  case object Write extends Operation

  private def isGranted(categSlug: String, op: Operation)(using ctx: UserContext): Fu[Boolean] =
    ForumCateg.slugToTeamId(categSlug).fold(fuTrue) { teamId =>
      teamCached.forumAccess get teamId flatMap {
        case Team.Access.NONE     => fuFalse
        case Team.Access.EVERYONE =>
          // when the team forum is open to everyone, you still need to belong to the team in order to post
          op match
            case Read  => fuTrue
            case Write => ctx.userId ?? { teamApi.belongsTo(teamId, _) }
        case Team.Access.MEMBERS => ctx.userId ?? { teamApi.belongsTo(teamId, _) }
        case Team.Access.LEADERS => ctx.userId ?? { teamApi.leads(teamId, _) }
      }
    }

  def isGrantedRead(categSlug: String)(using ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.Shusher)) fuTrue
    else isGranted(categSlug, Read)

  def isGrantedWrite(categSlug: String, tryingToPostAsMod: Boolean = false)(using
      ctx: UserContext
  ): Fu[Boolean] =
    if (tryingToPostAsMod && ctx.me ?? Granter(Permission.Shusher)) fuTrue
    else ctx.me.exists(canWriteInAnyForum) ?? isGranted(categSlug, Write)

  private def canWriteInAnyForum(u: User) =
    !u.isBot && {
      (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }

  def isGrantedMod(categSlug: String)(using ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.ModerateForum)) fuTrue
    else
      ForumCateg.slugToTeamId(categSlug) ?? { teamId =>
        ctx.userId ?? {
          teamApi.leads(teamId, _)
        }
      }
