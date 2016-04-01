package lila.setup

import reactivemongo.api._
import reactivemongo.bson._

import lila.common.LilaException
import lila.db.dsl._
import lila.db.Implicits._
import lila.game.Game
import lila.user.User
import tube.userConfigTube

private[setup] object UserConfigRepo {

  def update(user: User)(f: UserConfig => UserConfig): Funit =
    config(user) flatMap { config =>
      userConfigTube.coll.update(
        BSONDocument("_id" -> config.id),
        f(config),
        upsert = true).void
    }

  def config(user: User): Fu[UserConfig] =
    $find byId user.id recover {
      case e: LilaException => {
        logger.warn("Can't load config", e)
        none[UserConfig]
      }
    } map (_ | UserConfig.default(user.id))

  def filter(user: User): Fu[FilterConfig] =
    userConfigTube.coll.find(
      BSONDocument("_id" -> user.id),
      BSONDocument("filter" -> true)
    ).one[BSONDocument] map {
        _ flatMap (_.getAs[FilterConfig]("filter")) getOrElse FilterConfig.default
      }
}
