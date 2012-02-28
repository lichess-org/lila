package lila
package repo

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

class GameRepo(collection: MongoCollection) extends SalatDAO[Game, String](collection) {

  def game(id: String) = findOneByID(id)
}
