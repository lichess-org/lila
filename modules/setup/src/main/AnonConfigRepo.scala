package lila.setup

import lila.user.User

import tube.{ anonConfigTube, filterConfigTube }
import lila.game.Game
import lila.db.Implicits._
import lila.db.api._
import lila.common.LilaCookie

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import reactivemongo.api._
import reactivemongo.bson._

private[setup] object AnonConfigRepo {

  private val sessionKey = "setup"

  def update(req: RequestHeader)(map: UserConfig ⇒ UserConfig): Funit =
    configOption(req) flatMap { co ⇒
      co.zmap(config ⇒ $save(map(config)))
    }

  def config(req: RequestHeader): Fu[UserConfig] =
    configOption(req) map (_ | UserConfig.default("nocookie"))

  def config(sid: String): Fu[UserConfig] =
    $find byId sid recover {
      case e: lila.db.DbException ⇒ {
        logwarn("Can't load config: " + e.getMessage)
        none[UserConfig]
      }
    } map (_ | UserConfig.default(sid))

  private def configOption(req: RequestHeader): Fu[Option[UserConfig]] =
    sessionId(req).zmap(s ⇒ config(s) map (_.some))

  def filter(req: RequestHeader): Fu[FilterConfig] = sessionId(req) zmap { sid ⇒
    $primitive.one($select(sid), "filter")(_.asOpt[FilterConfig])
  } map (_ | FilterConfig.default)

  private def sessionId(req: RequestHeader): Option[String] =
    req.session.get(LilaCookie.sessionId)
}
