package lila.api

import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.Syncache

final class AssetVersionApi(
    initialVersion: Int,
    coll: Coll)(implicit system: akka.actor.ActorSystem) {

  def get = cache sync true

  private var lastVersion = initialVersion

  private val cache = new Syncache[Boolean, Int](
    name = "asset.version",
    compute = _ => coll.primitiveOne[BSONNumberLike]($id("asset"), "version").map {
    _.fold(lastVersion)(_.toInt max initialVersion)
  } addEffect { version =>
    lastVersion = version
  },
    default = _ => lastVersion,
    strategy = Syncache.NeverWait,
    timeToLive = 5.seconds,
    logger = lila.log("assetVersion"))(system)
}
