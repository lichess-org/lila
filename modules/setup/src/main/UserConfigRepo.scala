package lila.setup

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

private[setup] object UserConfigRepo {

  // dirty
  private val coll = Env.current.userConfigColl

  def update(user: User)(f: UserConfig => UserConfig): Funit =
    config(user) flatMap { config =>
      coll.update.one(
        $id(config.id),
        f(config),
        upsert = true
      ).void
    }

  def config(user: User): Fu[UserConfig] =
    coll.byId[UserConfig](user.id) recover {
      case e: Exception => {
        logger.warn("Can't load config", e)
        none[UserConfig]
      }
    } map (_ | UserConfig.default(user.id))

  def filter(user: User): Fu[FilterConfig] =
    coll.primitiveOne[FilterConfig]($id(user.id), "filter") map (_ | FilterConfig.default)
}
