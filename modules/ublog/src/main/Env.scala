package lila.ublog

import com.github.blemale.scaffeine.AsyncLoadingCache
import com.softwaremill.macwire.*

import lila.core.config.*
import lila.db.dsl.Coll
import lila.common.Bus

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    picfitApi: lila.memo.PicfitApi,
    ircApi: lila.core.irc.IrcApi,
    relationApi: lila.core.relation.RelationApi,
    shutupApi: lila.core.shutup.ShutupApi,
    captcha: lila.core.captcha.CaptchaApi,
    cacheApi: lila.memo.CacheApi,
    langList: lila.core.i18n.LangList,
    net: NetConfig
)(using Executor, Scheduler, akka.stream.Materializer, play.api.Mode):

  export net.{ assetBaseUrl, baseUrl, domain, assetDomain }

  private val colls = new UblogColls(db(CollName("ublog_blog")), db(CollName("ublog_post")))

  val topic = wire[UblogTopicApi]

  val rank: UblogRank = wire[UblogRank]

  val api: UblogApi = wire[UblogApi]

  val paginator = wire[UblogPaginator]

  val bestOf = wire[UblogBestOfApi]

  val markup = wire[UblogMarkup]

  val form = wire[UblogForm]

  val viewCounter = wire[UblogViewCounter]

  val lastPostsCache: AsyncLoadingCache[Unit, List[UblogPost.PreviewPost]] =
    cacheApi.unit[List[UblogPost.PreviewPost]]:
      _.refreshAfterWrite(10.seconds).buildAsyncFuture: _ =>
        import scalalib.ThreadLocalRandom
        val lookInto = 15
        val keep     = 9
        api
          .pinnedPosts(2)
          .zip:
            api
              .latestPosts(lookInto)
              .map:
                _.groupBy(_.blog)
                  .flatMap(_._2.headOption)
              .map(ThreadLocalRandom.shuffle)
              .map(_.take(keep).toList)
          .map(_ ++ _)

  Bus.subscribeFun("shadowban"):
    case lila.core.mod.Shadowban(userId, v) =>
      api.setShadowban(userId, v) >>
        rank.recomputeRankOfAllPostsOfBlog(UblogBlog.Id.User(userId))

  import lila.core.security.ReopenAccount
  Bus.sub[ReopenAccount]:
    case ReopenAccount(user) => api.onAccountReopen(user)

final private class UblogColls(val blog: Coll, val post: Coll)
