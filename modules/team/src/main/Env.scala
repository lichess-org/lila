package lila.team

import akka.actor._
import com.softwaremill.macwire._

import lila.common.config._
import lila.mod.ModlogApi
import lila.notify.NotifyApi

final class Env(
    captcher: lila.hub.actors.Captcher,
    timeline: lila.hub.actors.Timeline,
    teamSearch: lila.hub.actors.TeamSearch,
    userRepo: lila.user.UserRepo,
    modLog: ModlogApi,
    notifyApi: NotifyApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
)(implicit system: ActorSystem) {

  private val teamRepo = new TeamRepo(db(CollName("team")))
  private val memberRepo = new MemberRepo(db(CollName("team_member")))
  private val requestRepo = new RequestRepo(db(CollName("team_request")))

  lazy val forms = wire[DataForm]

  lazy val memberStream = wire[TeamMemberStream]

  lazy val api = wire[TeamApi]

  lazy val paginator = wire[PaginatorBuilder]

  lazy val cli = wire[Cli]

  lazy val cached: Cached = wire[Cached]

  private lazy val notifier = wire[Notifier]

  lazy val getTeamName = new GetTeamName(cached.nameCache.sync _)

  lila.common.Bus.subscribeFun("shadowban") {
    case lila.hub.actorApi.mod.Shadowban(userId, true) => api deleteRequestsByUserId userId
  }
}
