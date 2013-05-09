package lila.setup

import lila.user.User
import tube.{ userConfigTube, filterConfigTube }
import lila.game.Game
import lila.db.Implicits._
import lila.db.api._
import lila.common.LilaException

import play.api.libs.json._

import reactivemongo.api._
import reactivemongo.bson._

private[setup] object UserConfigRepo {

  def update(user: User)(map: UserConfig ⇒ UserConfig): Funit =
    config(user) flatMap { c ⇒ $save(map(c)) }

  def config(user: User): Fu[UserConfig] =
    $find byId user.id recover {
      case e: LilaException ⇒ {
        logwarn("Can't load config: " + e.getMessage)
        none[UserConfig]
      }
    } map (_ | UserConfig.default(user.id))

  def filter(user: User): Fu[FilterConfig] = $primitive.one(
    $select(user.id),
    "filter")(_.asOpt[FilterConfig]) map (_ | FilterConfig.default)
}
