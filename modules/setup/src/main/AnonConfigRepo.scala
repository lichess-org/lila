package lila.setup

import play.api.libs.json._
import play.api.mvc._
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.{ LilaCookie, LilaException }
import lila.db.api._
import lila.db.Implicits._
import lila.game.Game
import lila.user.User
import tube.{ anonConfigTube, filterConfigTube }

private[setup] object AnonConfigRepo {

  private val sessionKey = "setup"

  def update(req: RequestHeader)(map: UserConfig => UserConfig): Funit =
    configOption(req) flatMap { co =>
      co.??(config => $save(map(config)))
    }

  def config(req: RequestHeader): Fu[UserConfig] =
    configOption(req) map (_ | UserConfig.default("nocookie"))

  def config(sid: String): Fu[UserConfig] =
    $find byId sid recover {
      case e: LilaException => {
        logwarn("Can't load config: " + e.getMessage)
        none[UserConfig]
      }
    } map (_ | UserConfig.default(sid))

  private def configOption(req: RequestHeader): Fu[Option[UserConfig]] =
    sessionId(req).??(s => config(s) map (_.some))

  def filter(req: RequestHeader): Fu[FilterConfig] = sessionId(req) ?? { sid =>
    $primitive.one($select(sid), "filter")(_.asOpt[FilterConfig])
  } map (_ | FilterConfig.default)

  private def sessionId(req: RequestHeader): Option[String] =
    req.session.get(LilaCookie.sessionId)
}
