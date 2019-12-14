package lila.setup

import play.api.mvc._
import reactivemongo.api.bson._

import lila.db.dsl._

final private class AnonConfigRepo(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def update(req: RequestHeader)(f: UserConfig => UserConfig): Funit =
    configOption(req) flatMap {
      _ ?? { config =>
        coll.update
          .one(
            $id(config.id),
            f(config),
            upsert = true
          )
          .void
      }
    }

  def config(req: RequestHeader): Fu[UserConfig] =
    configOption(req) map (_ | UserConfig.default("nocookie"))

  def config(sid: String): Fu[UserConfig] =
    coll.byId[UserConfig](sid) recover {
      case e: Exception => {
        logger.warn("Can't load config", e)
        none[UserConfig]
      }
    } dmap (_ | UserConfig.default(sid))

  private def configOption(req: RequestHeader): Fu[Option[UserConfig]] =
    sessionId(req).??(s => config(s) map (_.some))

  def filter(req: RequestHeader): Fu[FilterConfig] =
    sessionId(req) ?? { sid =>
      coll.primitiveOne[FilterConfig]($id(sid), "filter")
    } dmap (_ | FilterConfig.default)

  private def sessionId(req: RequestHeader): Option[String] =
    lila.common.HTTPRequest sid req
}
