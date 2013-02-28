package lila.app
package setup

import http.LilaCookie

import play.api.mvc._

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

private[setup] final class AnonConfigRepo(collection: MongoCollection)
    extends SalatDAO[RawUserConfig, String](collection) {

  private val sessionKey = "setup"

  def update(req: RequestHeader)(map: UserConfig ⇒ UserConfig): IO[Unit] =
    configOption(req) flatMap { co ⇒
      ~co.map(config ⇒ save(map(config)))
    }

  def config(req: RequestHeader): IO[UserConfig] =
    configOption(req) map (_ | UserConfig.default("nocookie"))

  private def config(sid: String): IO[UserConfig] = {
    io {
      findOneById(sid) flatMap (_.decode)
    } except { e ⇒
      putStrLn("Can't load config: " + e.getMessage) map (_ ⇒ none[UserConfig])
    }
  } map (_ | UserConfig.default(sid))

  private def configOption(req: RequestHeader): IO[Option[UserConfig]] =
    ~sessionId(req).map(s ⇒ config(s) map (_.some))

  def filter(req: RequestHeader): IO[FilterConfig] = io {
    for {
      sid ← sessionId(req)
      obj ← collection.findOneByID(sid, DBObject("filter" -> true))
      config ← FilterConfig fromDB obj
    } yield config
  } map (_ | FilterConfig.default)

  private def save(config: UserConfig): IO[Unit] = io {
    update(
      DBObject("_id" -> config.id),
      _grater asDBObject config.encode,
      upsert = true)
  }

  private def sessionId(req: RequestHeader): Option[String] =
    req.session.get(LilaCookie.sessionId)
}
