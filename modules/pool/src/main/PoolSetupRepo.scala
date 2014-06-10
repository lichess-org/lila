package lila.pool

import com.typesafe.config.{ Config, ConfigObject }
import lila.common.PimpedConfig._
import scala.collection.JavaConversions._
import scala.util.Try

private[pool] final class PoolSetupRepo(config: Config) {

  lazy val setups: List[PoolSetup] = config.root.map {
    case (id, obj: ConfigObject) =>
      val conf = obj.toConfig
      for {
        name <- Try(conf getString "name").toOption
        clock <- Option(conf getConfig "clock")
        clockLimit <- Try(clock getInt "limit").toOption
        clockIncrement <- Try(clock getInt "increment").toOption
        variant <- Try(conf getString "variant").toOption flatMap chess.Variant.apply
      } yield PoolSetup(id, name, clockLimit, clockIncrement, variant)
    case _ => none
  }.toList.flatten.sortBy(_.id)

  val setupMap = setups.map { p =>
    p.id -> p
  }.toMap

  def byId(id: ID) = setupMap get id

  def exists(id: ID) = setupMap contains id
}
