package lila
package db

import model.Hook

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

class HookRepo(collection: MongoCollection)
    extends SalatDAO[Hook, String](collection) {

  def hook(hookId: String): IO[Option[Hook]] = io {
    findOneByID(hookId)
  }

  def ownedHook(ownerId: String): IO[Option[Hook]] = io {
    findOne(DBObject("ownerId" -> ownerId))
  }

  def allOpen = hookList(DBObject(
    "match" -> false
  ))

  def allOpenCasual = hookList(DBObject(
    "match" -> false,
    "mode" -> 0
  ))

  def hookList(query: DBObject): IO[List[Hook]] = io {
    find(query) sort DBObject("createdAt" -> 1) toList
  }

  def removeId(id: String): IO[Unit] = io {
    remove(DBObject("_id" -> id))
  }

  def removeOwnerId(ownerId: String): IO[Unit] = io {
    remove(DBObject("ownerId" -> ownerId))
  }

  def keepOnlyOwnerIds(ids: Iterable[String]): IO[Boolean] = io {
    val removableIds = primitiveProjection[String](
      ("ownerId" $nin ids) ++ ("match" -> false), "_id"
    ).toList
    if (removableIds.nonEmpty) {
      remove("_id" $in removableIds)
      true
    }
    else false
  }

  def cleanupOld: IO[Unit] = io {
    remove("createdAt" $lt (DateTime.now - 1.hour))
  }
}
