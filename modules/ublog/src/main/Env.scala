package lila.ublog

import com.github.blemale.scaffeine.AsyncLoadingCache
import com.softwaremill.macwire.*

import lila.common.config.*
import lila.db.dsl.Coll

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    timeline: lila.hub.actors.Timeline,
    picfitApi: lila.memo.PicfitApi,
    ircApi: lila.irc.IrcApi,
    relationApi: lila.relation.RelationApi,
    shutup: lila.hub.actors.Shutup,
    captcher: lila.hub.actors.Captcher,
    cacheApi: lila.memo.CacheApi,
    net: NetConfig
)(using Executor, Scheduler, akka.stream.Materializer, play.api.Mode):

  export net.{ assetBaseUrl, baseUrl, domain, assetDomain }

  private val colls = new UblogColls(db(CollName("ublog_blog")), db(CollName("ublog_post")))

  val topic = wire[UblogTopicApi]

  val rank: UblogRank = wire[UblogRank]

  val api: UblogApi = wire[UblogApi]

  val paginator = wire[UblogPaginator]

  val markup = wire[UblogMarkup]

  val form = wire[UblogForm]

  val viewCounter = wire[UblogViewCounter]

  val lastPostsCache: AsyncLoadingCache[Unit, List[UblogPost.PreviewPost]] =
    cacheApi.unit[List[UblogPost.PreviewPost]]:
      _.refreshAfterWrite(10 seconds).buildAsyncFuture: _ =>
        import ornicar.scalalib.ThreadLocalRandom
        val lookInto = 5
        val keep     = 2
        api
          .latestPosts(lookInto)
          .map:
            _.groupBy(_.blog)
              .flatMap(_._2.headOption)
          .map(ThreadLocalRandom.shuffle)
          .map(_.take(keep).toList)

  lila.common.Bus.subscribeFun("shadowban"):
    case lila.hub.actorApi.mod.Shadowban(userId, v) =>
      api.setShadowban(userId, v) >>
        rank.recomputeRankOfAllPostsOfBlog(UblogBlog.Id.User(userId))

final private class UblogColls(val blog: Coll, val post: Coll)
