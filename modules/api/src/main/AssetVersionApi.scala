package lila.api

import reactivemongo.bson._
import scala.concurrent.duration._

import lila.common.AssetVersion
import lila.db.dsl._
import lila.memo.Syncache

final class AssetVersionApi(
    initialVersion: AssetVersion,
    coll: Coll
)(implicit system: akka.actor.ActorSystem) {

  def get: AssetVersion = cache sync true

  private var lastVersion = initialVersion

  private val cache = new Syncache[Boolean, AssetVersion](
    name = "asset.version",
    compute = _ => coll.primitiveOne[BSONNumberLike]($id("asset"), "version").map {
      _.fold(lastVersion) { v =>
        AssetVersion(v.toInt max initialVersion.value)
      }
    } addEffect { version =>
      lastVersion = version
    },
    default = _ => lastVersion,
    strategy = Syncache.NeverWait,
    expireAfter = Syncache.ExpireAfterWrite(5 seconds),
    logger = lila.log("assetVersion")
  )(system)
}
