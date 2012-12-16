package lila
package setup

import play.api.mvc.{ Cookie, RequestHeader }

final class AnonConfigRepo {

  private val name = "setup"

  def update(map: UserConfig ⇒ UserConfig)(implicit req: RequestHeader): Cookie =
    LilaCookie.session(name, value.toString)(ctx.req)
    config(user) flatMap { c ⇒ save(map(c)) }

  def config(implicit req: RequestHeader): IO[UserConfig] = io {
    findOneById(user.id) flatMap (_.decode)
  } except { e ⇒
    putStrLn("Can't load config: " + e.getMessage) inject none[UserConfig]
  } map (_ | UserConfig.default(user))

  def save(config: UserConfig)(implicit req: RequestHeader): IO[Unit] = io {
    update(
      DBObject("_id" -> config.id),
      _grater asDBObject config.encode,
      upsert = true)
  }
}
