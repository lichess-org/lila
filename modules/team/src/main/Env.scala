package lila.team

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }
import lila.common.Bus

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
    db: lila.db.Db,
    mongoRateLimitApi: lila.memo.MongoRateLimitApi
)(using Executor, Scheduler, akka.stream.Materializer):

  lazy val teamRepo = TeamRepo(db(CollName("team")))
  lazy val memberRepo = TeamMemberRepo(db(CollName("team_member")))
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

  lazy val limiter = wire[TeamLimiter]

  lazy val security = wire[TeamSecurity]

  lazy val api = wire[TeamApi]

  wire[TeamClasSync]

  lila.common.Cli.handle:
    case "team" :: "members" :: "add" :: teamId :: members :: Nil =>
      for
        team <- teamRepo.byId(TeamId(teamId)).orFail(s"Team $teamId not found")
        userIds = members.split(',').flatMap(UserStr.read).map(_.id).toList
        _ <- api.addMembers(team, userIds)
      yield s"Added ${userIds.size} members to team ${team.name}"

  Bus.sub[lila.core.mod.Shadowban]:
    case lila.core.mod.Shadowban(userId, true) =>
      api.deleteRequestsByUserId(userId)

  Bus.sub[lila.core.team.IsLeaderWithCommPerm]:
    case lila.core.team.IsLeaderWithCommPerm(teamId, userId, promise) =>
      promise.completeWith(api.hasPerm(teamId, userId, _.Comm))

  Bus.sub[lila.core.team.TeamIdsJoinedBy]:
    case lila.core.team.TeamIdsJoinedBy(userId, promise) =>
      promise.completeWith(cached.teamIdsList(userId))

  Bus.sub[lila.core.team.IsLeaderOf]:
    case lila.core.team.IsLeaderOf(leaderId, memberId, promise) =>
      promise.completeWith(api.isLeaderOf(leaderId, memberId))
