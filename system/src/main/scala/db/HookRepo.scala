package lila.system
package db

import model.Hook

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

class HookRepo(collection: MongoCollection)
    extends SalatDAO[Hook, String](collection) {

  def allOpen = hookList(DBObject(
    "match" -> false
  ))

  def allOpenCasual = hookList(DBObject(
    "match" -> false,
    "mode" -> 0
  ))

  def hookList(query: MongoDBObject): IO[List[Hook]] = io {
    find(query) sort DBObject("createdAt" -> 1) toList
  }
}
