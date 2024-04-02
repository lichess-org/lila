package lila.forum

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.autoconfig.{ *, given }
import lila.core.config.*
import lila.notify.NotifyApi
import lila.pref.PrefApi
import lila.core.relation.RelationApi
import lila.user.User
import lila.core.forum.ForumPostMiniView

@Module
final private class ForumConfig(
    @ConfigName("topic.max_per_page") val topicMaxPerPage: MaxPerPage,
    @ConfigName("post.max_per_page") val postMaxPerPage: MaxPerPage
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    spam: lila.security.Spam,
    promotion: lila.security.PromotionApi,
    captcha: lila.core.captcha.CaptchaApi,
    shutupApi: lila.core.shutup.ShutupApi,
    notifyApi: NotifyApi,
    relationApi: RelationApi,
    prefApi: PrefApi,
    modLog: lila.core.mod.LogApi,
    userRepo: lila.user.UserRepo,
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
    postRepo.recentIdsInCateg(ForumCateg.fromTeamId(id), 6).flatMap(postApi.miniViews)

  lila.common.Bus.subscribeFun("team", "gdprErase"):
    case lila.core.team.TeamCreate(t)   => categApi.makeTeam(t.id, t.name, t.userId)
    case lila.user.User.GDPRErase(user) => postApi.eraseFromSearchIndex(user)

private type RecentTeamPostsType                   = TeamId => Fu[List[ForumPostMiniView]]
opaque type RecentTeamPosts <: RecentTeamPostsType = RecentTeamPostsType
object RecentTeamPosts extends TotalWrapper[RecentTeamPosts, RecentTeamPostsType]
