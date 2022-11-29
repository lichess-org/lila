package lila.relation

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.hub.actors

@Module
private class RelationConfig(
    @ConfigName("collection.relation") val collection: CollName,
    @ConfigName("limit.follow") val maxFollow: Max,
    @ConfigName("limit.block") val maxBlock: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    timeline: actors.Timeline,
    userRepo: lila.user.UserRepo,
    prefApi: lila.pref.PrefApi,
    cacheApi: lila.memo.CacheApi
)(using
    scala.concurrent.ExecutionContext,
    ActorSystem,
    akka.stream.Materializer
):

  private val config = appConfig.get[RelationConfig]("relation")(AutoConfig.loader)

  def maxFollow = config.maxFollow

  private lazy val coll = db(config.collection)

  private lazy val repo: RelationRepo = wire[RelationRepo]

  lazy val api: RelationApi = wire[RelationApi]

  lazy val stream = wire[RelationStream]
