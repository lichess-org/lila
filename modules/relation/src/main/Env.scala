package lila.relation

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.common.config.*
import lila.db.dsl.Coll
import lila.hub.actors

@Module
private class RelationConfig(
    @ConfigName("collection.relation") val relation: CollName,
    @ConfigName("collection.subscription") val subscription: CollName,
    @ConfigName("limit.follow") val maxFollow: Max,
    @ConfigName("limit.block") val maxBlock: Max
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    timeline: actors.Timeline,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    prefApi: lila.pref.PrefApi,
    cacheApi: lila.memo.CacheApi
)(using
    Executor,
    ActorSystem,
    akka.stream.Materializer
):

  private val config = appConfig.get[RelationConfig]("relation")(AutoConfig.loader)

  export config.maxFollow

  private lazy val colls = Colls(db(config.relation), db(config.subscription))

  private lazy val repo: RelationRepo = wire[RelationRepo]

  lazy val subs: SubscriptionRepo = wire[SubscriptionRepo]

  lazy val api: RelationApi = wire[RelationApi]

  lazy val stream = wire[RelationStream]

final case class Colls(relation: Coll, subscription: Coll)
