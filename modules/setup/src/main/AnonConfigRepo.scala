package lila.setup

import play.api.mvc._
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.{ LilaCookie, LilaException }
import lila.db.dsl._
import lila.db.Implicits._
import lila.game.Game
import lila.user.User
import tube.anonConfigTube

private[setup] object AnonConfigRepo {

  def update(req: RequestHeader)(f: UserConfig => UserConfig): Funit =
    configOption(req) flatMap {
      _ ?? { config =>
        anonConfigTube.coll.update(
          BSONDocument("_id" -> config.id),
          f(config),
          upsert = true).void
      }
    }

  def config(req: RequestHeader): Fu[UserConfig] =
    configOption(req) map (_ | UserConfig.default("nocookie"))

  def config(sid: String): Fu[UserConfig] =
    $find byId sid recover {
      case e: LilaException => {
        logger.warn("Can't load config", e)
        none[UserConfig]
      }
    } map (_ | UserConfig.default(sid))

  private def configOption(req: RequestHeader): Fu[Option[UserConfig]] =
    sessionId(req).??(s => config(s) map (_.some))

  def filter(req: RequestHeader): Fu[FilterConfig] = sessionId(req) ?? { sid =>
    anonConfigTube.coll.find(
      BSONDocument("_id" -> sid),
      BSONDocument("filter" -> true)
    ).one[BSONDocument] map {
        _ flatMap (_.getAs[FilterConfig]("filter"))
      }
  } map (_ | FilterConfig.default)

  private def sessionId(req: RequestHeader): Option[String] =
    lila.common.HTTPRequest sid req
}
