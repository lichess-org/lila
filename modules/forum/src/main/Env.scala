package lila.forum

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import lila.hub.actorApi.team.CreateTeam
import lila.mod.ModlogApi
import lila.notify.NotifyApi
import lila.pref.PrefApi
import lila.relation.RelationApi
import lila.user.User

@Module
final private class ForumConfig(
    @ConfigName("topic.max_per_page") val topicMaxPerPage: MaxPerPage,
    @ConfigName("post.max_per_page") val postMaxPerPage: MaxPerPage
)

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    modLog: ModlogApi,
    spam: lila.security.Spam,
    promotion: lila.security.PromotionApi,
    captcher: lila.hub.actors.Captcher,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    forumSearch: lila.hub.actors.ForumSearch,
    notifyApi: NotifyApi,
    relationApi: RelationApi,
    prefApi: PrefApi,
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo,
    cacheApi: lila.memo.CacheApi,
    ws: StandaloneWSClient
)(using Executor, Scheduler, akka.stream.Materializer):

  private val config = appConfig.get[ForumConfig]("forum")(AutoConfig.loader)

  lazy val categRepo = new ForumCategRepo(db(CollName("f_categ")))
  lazy val topicRepo = new ForumTopicRepo(db(CollName("f_topic")))
  lazy val postRepo  = new ForumPostRepo(db(CollName("f_post")))

  private lazy val detectLanguage =
    DetectLanguage(ws, appConfig.get[DetectLanguage.Config]("detectlanguage.api"))

  private lazy val textExpand = wire[ForumTextExpand]

  lazy val paginator: ForumPaginator = wire[ForumPaginator]

  lazy val categApi: ForumCategApi = wire[ForumCategApi]

  lazy val topicApi: ForumTopicApi = wire[ForumTopicApi]

  lazy val postApi: ForumPostApi = wire[ForumPostApi]

  lazy val delete: ForumDelete = wire[ForumDelete]

  lazy val mentionNotifier: MentionNotifier = wire[MentionNotifier]
  lazy val forms                            = wire[ForumForm]

  lazy val recentTeamPosts = RecentTeamPosts: id =>
    postRepo.recentInCateg(ForumCateg.fromTeamId(id), 6) flatMap postApi.miniPosts

  lila.common.Bus.subscribeFun("team", "gdprErase"):
    case CreateTeam(id, name, _)        => categApi.makeTeam(id, name)
    case lila.user.User.GDPRErase(user) => postApi.eraseFromSearchIndex(user)

private type RecentTeamPostsType                   = TeamId => Fu[List[MiniForumPost]]
opaque type RecentTeamPosts <: RecentTeamPostsType = RecentTeamPostsType
object RecentTeamPosts extends TotalWrapper[RecentTeamPosts, RecentTeamPostsType]
