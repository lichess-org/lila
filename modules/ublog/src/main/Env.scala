package lila.ublog

import com.softwaremill.macwire._

import lila.common.config._

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

  private val postColl = db(CollName("ublog_post"))

  val api = wire[UblogApi]

  val like = wire[UblogLike]

  val paginator = wire[UblogPaginator]

  val markup = wire[UblogMarkup]

  val form = wire[UblogForm]
}
