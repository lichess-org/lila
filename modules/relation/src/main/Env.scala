package lila.relation

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.config.*
import lila.db.dsl.Coll

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
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    prefApi: lila.core.pref.PrefApi,
    cacheApi: lila.memo.CacheApi
)(using Executor, ActorSystem, akka.stream.Materializer, lila.core.config.RateLimit):

  private val config = appConfig.get[RelationConfig]("relation")(AutoConfig.loader)

  export config.maxFollow

  private lazy val colls = Colls(db(config.relation), db(config.subscription))

  private lazy val repo: RelationRepo = wire[RelationRepo]

  lazy val subs: SubscriptionRepo = wire[SubscriptionRepo]

  lazy val api: RelationApi = wire[RelationApi]

  lazy val stream = wire[RelationStream]

final case class Colls(relation: Coll, subscription: Coll)
