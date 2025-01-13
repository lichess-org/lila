package lila.team

import akka.actor.*
import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    captcha: lila.core.captcha.CaptchaApi,
    userApi: lila.core.user.UserApi,
    flairApi: lila.core.user.FlairApi,
    notifyApi: lila.core.notify.NotifyApi,
    socketKit: lila.core.socket.SocketKit,
    chat: lila.core.chat.ChatApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.core.user.LightUserApi,
    userJson: lila.core.user.JsonView,
    db: lila.db.Db
)(using Executor, ActorSystem, play.api.Mode, akka.stream.Materializer, lila.core.user.FlairGet):

  lazy val teamRepo    = TeamRepo(db(CollName("team")))
  lazy val memberRepo  = TeamMemberRepo(db(CollName("team_member")))
  lazy val requestRepo = TeamRequestRepo(db(CollName("team_request")))

  lazy val forms = wire[TeamForm]

  lazy val memberStream = wire[TeamMemberStream]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val cached: Cached = wire[Cached]

  lazy val jsonView = wire[JsonView]

  private val teamSocket = wire[TeamSocket]

  def version(teamId: TeamId) = teamSocket.rooms.ask[SocketVersion](teamId.into(RoomId))(GetVersion.apply)

  private lazy val notifier = wire[Notifier]

  export cached.lightApi as lightTeamApi

  export cached.{ async as lightTeam, sync as lightTeamSync }

  lazy val security = wire[TeamSecurity]

  lazy val api = wire[TeamApi]

  def cli: lila.common.Cli = new:
    def process =
      case "team" :: "members" :: "add" :: teamId :: members :: Nil =>
        for
          team <- teamRepo.byId(TeamId(teamId)).orFail(s"Team $teamId not found")
          userIds = members.split(',').flatMap(UserStr.read).map(_.id).toList
          _ <- api.addMembers(team, userIds)
        yield s"Added ${userIds.size} members to team ${team.name}"

  lila.common.Bus.subscribeFuns(
    "shadowban" -> { case lila.core.mod.Shadowban(userId, true) =>
      api.deleteRequestsByUserId(userId)
    },
    "teamIsLeader" -> {
      case lila.core.team.IsLeader(teamId, userId, promise) =>
        promise.completeWith(api.isLeader(teamId, userId))
      case lila.core.team.IsLeaderWithCommPerm(teamId, userId, promise) =>
        promise.completeWith(api.hasPerm(teamId, userId, _.Comm))
    },
    "teamJoinedBy" -> { case lila.core.team.TeamIdsJoinedBy(userId, promise) =>
      promise.completeWith(cached.teamIdsList(userId))
    },
    "teamIsLeaderOf" -> { case lila.core.team.IsLeaderOf(leaderId, memberId, promise) =>
      promise.completeWith(api.isLeaderOf(leaderId, memberId))
    }
  )
