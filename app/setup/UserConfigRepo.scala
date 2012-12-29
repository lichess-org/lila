package lila
package setup

import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

private[setup] final class UserConfigRepo(collection: MongoCollection)
    extends SalatDAO[RawUserConfig, String](collection) {

  def update(user: User)(map: UserConfig ⇒ UserConfig): IO[Unit] =
    config(user) flatMap { c ⇒ save(map(c)) }

  def config(user: User): IO[UserConfig] = io {
    findOneById(user.id) flatMap (_.decode)
  } except { e ⇒
    putStrLn("Can't load config: " + e.getMessage) map (_ ⇒ none[UserConfig])
  } map (_ | UserConfig.default(user.id))

  def filter(user: User): IO[FilterConfig] = io {
    for {
      obj ← collection.findOneByID(user.id, DBObject("filter" -> true))
      variant ← obj.getAs[Int]("v")
      config ← RawFilterConfig(variant).decode
    } yield config
  } map (_ | FilterConfig.default)

  private def save(config: UserConfig): IO[Unit] = io {
    update(
      DBObject("_id" -> config.id),
      _grater asDBObject config.encode,
      upsert = true)
  }
}
