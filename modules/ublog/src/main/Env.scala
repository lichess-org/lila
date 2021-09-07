package lila.ublog

import com.softwaremill.macwire._

import lila.common.config._
import lila.db.dsl.Coll

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    timeline: lila.hub.actors.Timeline,
    picfitApi: lila.memo.PicfitApi,
    picfitUrl: lila.memo.PicfitUrl,
    ircApi: lila.irc.IrcApi,
    relationApi: lila.relation.RelationApi,
    captcher: lila.hub.actors.Captcher,
    cacheApi: lila.memo.CacheApi,
    net: NetConfig
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val colls = new UblogColls(db(CollName("ublog_blog")), db(CollName("ublog_post")))

  val api = wire[UblogApi]

  val like = wire[UblogLike]

  val paginator = wire[UblogPaginator]

  val markup = wire[UblogMarkup]

  val form = wire[UblogForm]

  val viewCounter = wire[UblogViewCounter]

  lila.common.Bus.subscribeFun("shadowban") { case lila.hub.actorApi.mod.Shadowban(userId, v) =>
    api.setShadowban(userId, v) >>
      like.recomputeRankOfAllPosts(UblogBlog.Id.User(userId))
    ()
  }
}

final private class UblogColls(val blog: Coll, val post: Coll)
