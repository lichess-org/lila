package lila.ublog

import cats.syntax.all.*

import com.github.blemale.scaffeine.AsyncLoadingCache
import com.softwaremill.macwire.*

import lila.common.config.*
import lila.db.dsl.Coll

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    timeline: lila.hub.actors.Timeline,
    picfitApi: lila.memo.PicfitApi,
    ircApi: lila.irc.IrcApi,
    relationApi: lila.relation.RelationApi,
    captcher: lila.hub.actors.Captcher,
    cacheApi: lila.memo.CacheApi,
    net: NetConfig
)(using
    ec: Executor,
    scheduler: Scheduler,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
):

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
            _.mapWithIndex: (post, i) =>
              (post, ThreadLocalRandom.nextInt(10 * (lookInto - i)))
            .sortBy(_._2)
              .take(keep)
              .map(_._1)

  lila.common.Bus.subscribeFun("shadowban") { case lila.hub.actorApi.mod.Shadowban(userId, v) =>
    api.setShadowban(userId, v) >>
      rank.recomputeRankOfAllPostsOfBlog(UblogBlog.Id.User(userId))
    ()
  }

final private class UblogColls(val blog: Coll, val post: Coll)
