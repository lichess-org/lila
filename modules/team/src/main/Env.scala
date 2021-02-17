package lila.team

import akka.actor._
import com.softwaremill.macwire._

import lila.common.config._
import lila.mod.ModlogApi
import lila.notify.NotifyApi
import lila.socket.Socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    captcher: lila.hub.actors.Captcher,
    timeline: lila.hub.actors.Timeline,
    teamSearch: lila.hub.actors.TeamSearch,
    userRepo: lila.user.UserRepo,
    modLog: ModlogApi,
    notifyApi: NotifyApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    chatApi: lila.chat.ChatApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    mode: play.api.Mode
) {

  lazy val teamRepo    = new TeamRepo(db(CollName("team")))
  lazy val memberRepo  = new MemberRepo(db(CollName("team_member")))
  lazy val requestRepo = new RequestRepo(db(CollName("team_request")))

  lazy val forms = wire[TeamForm]

  lazy val memberStream = wire[TeamMemberStream]

  lazy val api = wire[TeamApi]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val cli = wire[TeamCli]

  lazy val cached: Cached = wire[Cached]

  lazy val jsonView = wire[JsonView]

  private val teamSocket = wire[TeamSocket]

  def version(teamId: Team.ID) =
    teamSocket.rooms.ask[SocketVersion](teamId)(GetVersion)

  private lazy val notifier = wire[Notifier]

  lazy val getTeamName = new GetTeamName(cached.blockingTeamName)

  lila.common.Bus.subscribeFuns(
    "shadowban" -> { case lila.hub.actorApi.mod.Shadowban(userId, true) =>
      api.deleteRequestsByUserId(userId).unit
    },
    "teamIsLeader" -> { case lila.hub.actorApi.team.IsLeader(teamId, userId, promise) =>
      promise completeWith cached.isLeader(teamId, userId)
    },
    "teamJoinedBy" -> { case lila.hub.actorApi.team.TeamIdsJoinedBy(userId, promise) =>
      promise completeWith cached.teamIdsList(userId)
    },
    "teamIsLeaderOf" -> { case lila.hub.actorApi.team.IsLeaderOf(leaderId, memberId, promise) =>
      promise completeWith api.isLeaderOf(leaderId, memberId)
    }
  )
}
