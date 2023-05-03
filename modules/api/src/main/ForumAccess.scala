package lila.api

import lila.forum.ForumCateg
import lila.security.{ Granter, Permission }
import lila.team.Team
import lila.user.{ User, UserContext }

final class ForumAccess(teamApi: lila.team.TeamApi, teamCached: lila.team.Cached)(using
    Executor
):

  enum Operation:
    case Read, Write

  private def isGranted(categId: ForumCategId, op: Operation)(using ctx: UserContext): Fu[Boolean] =
    ForumCateg.toTeamId(categId).fold(fuTrue) { teamId =>
      teamCached.forumAccess get teamId flatMap {
        case Team.Access.NONE     => fuFalse
        case Team.Access.EVERYONE =>
          // when the team forum is open to everyone, you still need to belong to the team in order to post
          op match
            case Operation.Read  => fuTrue
            case Operation.Write => ctx.userId ?? { teamApi.belongsTo(teamId, _) }
        case Team.Access.MEMBERS => ctx.userId ?? { teamApi.belongsTo(teamId, _) }
        case Team.Access.LEADERS => ctx.userId ?? { teamApi.leads(teamId, _) }
      }
    }

  def isGrantedRead(categId: ForumCategId)(using ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.Shusher)) fuTrue
    else isGranted(categId, Operation.Read)

  def isGrantedWrite(categId: ForumCategId, tryingToPostAsMod: Boolean = false)(using
      ctx: UserContext
  ): Fu[Boolean] =
    if (tryingToPostAsMod && ctx.me ?? Granter(Permission.Shusher)) fuTrue
    else ctx.me.exists(canWriteInAnyForum) ?? isGranted(categId, Operation.Write)

  private def canWriteInAnyForum(u: User) =
    !u.isBot && {
      true // (u.count.game > 0 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified || u.isPatron
    }

  def isGrantedMod(categId: ForumCategId)(using ctx: UserContext): Fu[Boolean] =
    if (ctx.me ?? Granter(Permission.ModerateForum)) fuTrue
    else
      ForumCateg.toTeamId(categId) ?? { teamId =>
        ctx.userId ?? {
          teamApi.leads(teamId, _)
        }
      }
