package lila.pool

import com.typesafe.config.{ Config, ConfigObject }
import lila.common.PimpedConfig._
import scala.collection.JavaConversions._
import scala.util.Try

private[pool] final class PoolRepo(config: Config) {

  def pools: List[Pool] = config.root.map {
    case (id, obj: ConfigObject) =>
      val conf = obj.toConfig
      for {
        name <- Try(conf getString "name").toOption
        clock <- Option(conf getConfig "clock")
        clockLimit <- Try(clock getInt "limit").toOption
        clockIncrement <- Try(clock getInt "increment").toOption
        variant <- Try(conf getString "variant").toOption flatMap chess.Variant.apply
      } yield Pool(id, name, clockLimit, clockIncrement, variant)
    case _ => none
  }.toList.flatten

  private val poolMap = pools.map { p =>
    p.id -> p
  }.toMap

  def byId(id: ID) = poolMap get id

  def exists(id: ID) = poolMap contains id
}
