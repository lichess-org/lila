package lila.setup

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final private class UserConfigRepo(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def update(user: User)(f: UserConfig => UserConfig): Funit =
    config(user) flatMap { config =>
      coll.update
        .one(
          $id(config.id),
          f(config),
          upsert = true
        )
        .void
    }

  def config(user: User): Fu[UserConfig] =
    coll.byId[UserConfig](user.id) recover {
      case e: Exception => {
        logger.warn("Can't load config", e)
        none[UserConfig]
      }
    } dmap (_ | UserConfig.default(user.id))

  def filter(user: User): Fu[FilterConfig] =
    coll.primitiveOne[FilterConfig]($id(user.id), "filter") dmap (_ | FilterConfig.default)
}
