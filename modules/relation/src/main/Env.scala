package lila.relation

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.hub.actors
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
    timeline: actors.Timeline,
    userRepo: lila.user.UserRepo,
    prefApi: lila.pref.PrefApi,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[RelationConfig]("relation")(AutoConfig.loader)

  def maxFollow = config.maxFollow

  private lazy val colls = Colls(db(config.relation), db(config.subscription))

  private lazy val repo: RelationRepo = wire[RelationRepo]

  lazy val subs: SubscriptionRepo = wire[SubscriptionRepo]

  lazy val api: RelationApi = wire[RelationApi]

  lazy val stream = wire[RelationStream]
}

final case class Colls(relation: Coll, subscription: Coll)
