package lila.api

import lila.forum.ForumCateg
import lila.security.{ Granter, Permission }
import lila.team.Team
import lila.user.{ User, Me }

final class ForumAccess(teamApi: lila.team.TeamApi, teamCached: lila.team.Cached)(using
    Executor
):

  enum Operation:
    case Read, Write

  private def isGranted(categId: ForumCategId, op: Operation)(using me: Option[Me]): Fu[Boolean] =
    ForumCateg.toTeamId(categId).fold(fuTrue) { teamId =>
      teamCached.forumAccess get teamId flatMap {
        case Team.Access.NONE     => fuFalse
        case Team.Access.EVERYONE =>
          // when the team forum is open to everyone, you still need to belong to the team in order to post
          op match
            case Operation.Read  => fuTrue
            case Operation.Write => me so { teamApi.belongsTo(teamId, _) }
        case Team.Access.MEMBERS => me so { teamApi.belongsTo(teamId, _) }
        case Team.Access.LEADERS => me so { teamApi.leads(teamId, _) }
      }
    }

  def isGrantedRead(categId: ForumCategId)(using me: Option[Me]): Fu[Boolean] =
    if me exists Granter(Permission.Shusher) then fuTrue
    else isGranted(categId, Operation.Read)

  def isGrantedWrite(categId: ForumCategId, tryingToPostAsMod: Boolean = false)(using
      me: Option[Me]
  ): Fu[Boolean] =
    if tryingToPostAsMod && me.exists(Granter(Permission.Shusher)) then fuTrue
    else me.exists(canWriteInAnyForum) so isGranted(categId, Operation.Write)

  private def canWriteInAnyForum(me: Me) =
    !me.isBot && {
      (me.count.game > 0 && me.createdSinceDays(2)) || me.hasTitle || me.isVerified || me.isPatron
    }

  def isGrantedMod(categId: ForumCategId)(using me: Me): Fu[Boolean] =
    if Granter(Permission.ModerateForum)(me) then fuTrue
    else
      ForumCateg.toTeamId(categId) so {
        teamApi.leads(_, me)
      }
