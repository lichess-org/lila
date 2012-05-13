package lila
package setup

import model.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

class UserConfigRepo(collection: MongoCollection)
    extends SalatDAO[UserConfig, String](collection) {

  def update(user: User, map: UserConfig ⇒ UserConfig): IO[Unit] = for {
    c1 ← config(user)
    c2 = map(c1)
    _ ← io { save(c2) }
  } yield ()

  def config(user: User): IO[UserConfig] = io {
    findOneByID(user.usernameCanonical)
  } map (_ | UserConfig.default(user))
}
