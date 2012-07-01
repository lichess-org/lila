package lila
package setup

import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.{ MongoCollection, WriteConcern }
import com.mongodb.casbah.Imports._
import scalaz.effects._

class UserConfigRepo(collection: MongoCollection)
    extends SalatDAO[RawUserConfig, String](collection) {

  def update(user: User)(map: UserConfig ⇒ UserConfig): IO[Unit] = for {
    c1 ← config(user)
    c2 = map(c1)
    _ ← save(c2)
  } yield ()

  def config(user: User): IO[UserConfig] = io {
    findOneById(user.id) flatMap (_.decode)
  } except { e ⇒
    putStrLn("Can't load config: " + e.getMessage) map (_ ⇒ none[UserConfig])
  } map (_ | UserConfig.default(user))

  def save(config: UserConfig): IO[Unit] = io {
    update(
      DBObject("_id" -> config.id),
      _grater asDBObject config.encode,
      upsert = true)
  }
}
