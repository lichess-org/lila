package lila.team

import akka.actor.*
import com.softwaremill.macwire.*

import lila.common.config.*
import lila.mod.ModlogApi
import lila.notify.NotifyApi
import lila.socket.{ GetVersion, SocketVersion }

@Module
@annotation.nowarn("msg=unused")
final class Env(
    captcher: lila.hub.actors.Captcher,
    timeline: lila.hub.actors.Timeline,
    teamSearch: lila.hub.actors.TeamSearch,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    modLog: ModlogApi,
    notifyApi: NotifyApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    chatApi: lila.chat.ChatApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    userJson: lila.user.JsonView,
    db: lila.db.Db
)(using Executor, ActorSystem, play.api.Mode, akka.stream.Materializer):

  lazy val teamRepo    = TeamRepo(db(CollName("team")))
  lazy val memberRepo  = TeamMemberRepo(db(CollName("team_member")))
  lazy val requestRepo = TeamRequestRepo(db(CollName("team_request")))

  lazy val forms = wire[TeamForm]

  lazy val memberStream = wire[TeamMemberStream]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val cached: Cached = wire[Cached]

  lazy val jsonView = wire[JsonView]

  private val teamSocket = wire[TeamSocket]

  def version(teamId: TeamId) = teamSocket.rooms.ask[SocketVersion](teamId into RoomId)(GetVersion.apply)

  private lazy val notifier = wire[Notifier]

  val getTeamName = GetTeamNameSync(cached.blockingTeamName)

  lazy val security = wire[TeamSecurity]

  lazy val api = wire[TeamApi]

  def cli: lila.common.Cli = new:
    def process =
      case "team" :: "members" :: "add" :: teamId :: members :: Nil =>
        for
          team <- teamRepo byId TeamId(teamId) orFail s"Team $teamId not found"
          userIds = members.split(',').flatMap(UserStr.read).map(_.id)
          _ <- api.addMembers(team, userIds)
        yield s"Added ${userIds.size} members to team ${team.name}"

  lila.common.Bus.subscribeFuns(
    "shadowban" -> { case lila.hub.actorApi.mod.Shadowban(userId, true) =>
      api.deleteRequestsByUserId(userId)
    },
    "teamIsLeader" -> {
      case lila.hub.actorApi.team.IsLeader(teamId, userId, promise) =>
        promise completeWith api.isLeader(teamId, userId)
      case lila.hub.actorApi.team.IsLeaderWithCommPerm(teamId, userId, promise) =>
        promise completeWith api.hasPerm(teamId, userId, _.Comm)
    },
    "teamJoinedBy" -> { case lila.hub.actorApi.team.TeamIdsJoinedBy(userId, promise) =>
      promise completeWith cached.teamIdsList(userId)
    },
    "teamIsLeaderOf" -> { case lila.hub.actorApi.team.IsLeaderOf(leaderId, memberId, promise) =>
      promise completeWith api.isLeaderOf(leaderId, memberId)
    }
  )
