package lila.forum

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config._
import lila.forum.actorApi.RemovePost
import lila.hub.actorApi.team.CreateTeam
import lila.mod.ModlogApi
import lila.mon
import lila.notify.NotifyApi
import lila.pref.PrefApi
import lila.relation.RelationApi
import lila.user.User
import lila.game.PgnDump

@Module
final private class ForumConfig(
    @ConfigName("topic.max_per_page") val topicMaxPerPage: MaxPerPage,
    @ConfigName("post.max_per_page") val postMaxPerPage: MaxPerPage
)

@Module
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
)(implicit ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler) {

  private val config = appConfig.get[ForumConfig]("forum")(AutoConfig.loader)

  lazy val categRepo = new CategRepo(db(CollName("f_categ")))
  lazy val topicRepo = new TopicRepo(db(CollName("f_topic")))
  lazy val postRepo  = new PostRepo(db(CollName("f_post")))

  private lazy val detectLanguage =
    new DetectLanguage(ws, appConfig.get[DetectLanguage.Config]("detectlanguage.api"))

  private lazy val textExpand = wire[ForumTextExpand]

  lazy val paginator: ForumPaginator = wire[ForumPaginator]

  lazy val categApi: CategApi = wire[CategApi]

  lazy val topicApi: TopicApi = wire[TopicApi]

  lazy val postApi: PostApi = wire[PostApi]

  lazy val delete: ForumDelete = wire[ForumDelete]

  lazy val mentionNotifier: MentionNotifier = wire[MentionNotifier]
  lazy val forms                            = wire[ForumForm]

  lazy val recentTeamPosts = new RecentTeamPosts(id =>
    postRepo.recentInCateg(teamSlug(id), 6) flatMap postApi.miniPosts
  )

  lila.common.Bus.subscribeFun("team", "gdprErase") {
    case CreateTeam(id, name, _)        => categApi.makeTeam(id, name).unit
    case lila.user.User.GDPRErase(user) => postApi.eraseFromSearchIndex(user).unit
  }
}

final class RecentTeamPosts(f: String => Fu[List[MiniForumPost]])
    extends (String => Fu[List[MiniForumPost]]) {
  def apply(teamId: String) = f(teamId)
}
