package lila.api

import lila.common.AssetVersion
import lila.db.dsl._

import reactivemongo.bson._

final class AssetVersionApi(
    val fromConfig: AssetVersion,
    coll: Coll
)(implicit system: akka.actor.ActorSystem) {

  private var current: AssetVersion = fromConfig

  def get: AssetVersion = current

  def set(v: AssetVersion): Funit = {
    current = v
    coll.updateField(dbId, dbField, v.value).void
  }

  private val dbId = $id("asset")
  private val dbField = "version"

  coll.primitiveOne[BSONNumberLike](dbId, dbField) map2 { (v: BSONNumberLike) =>
    current = AssetVersion(v.toInt atLeast fromConfig.value)
  }
}
