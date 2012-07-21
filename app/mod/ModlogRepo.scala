package lila
package mod

import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.{ MongoCollection, WriteConcern }
import com.mongodb.casbah.Imports._
import scalaz.effects._

class ModlogRepo(collection: MongoCollection)
    extends SalatDAO[Modlog, String](collection) {

  def saveIO(modlog: Modlog): IO[Unit] = io {
    save(modlog)
  }
}
