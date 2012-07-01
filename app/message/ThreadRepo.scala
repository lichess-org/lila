package lila
package message

import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class ThreadRepo(
    collection: MongoCollection) extends SalatDAO[Thread, String](collection) {

  def byId(id: String): IO[Option[Thread]] = io {
    findOneByID(id)
  }

  def byUser(user: User): IO[List[Thread]] = io {
    find(userQuery(user)).sort(sortQuery).toList
  }

  def visibleByUser(user: User): IO[List[Thread]] = io {
    find(visibleByUserQuery(user)).sort(sortQuery).toList
  }

  def userNbUnread(user: User): IO[Int] = userNbUnread(user.id)

  def userNbUnread(userId: String): IO[Int] = io {
    val result = collection.mapReduce(
      mapFunction = """function() {
  var thread = this, nb = 0;
  thread.posts.forEach(function(p) {
    if (!p.isRead) {
      if (thread.creatorId == "%s") {
        if (!p.isByCreator) nb++;
      } else if (p.isByCreator) nb++;
    }
  });
  if (nb > 0) emit("n", nb);
}""" format userId,
      reduceFunction = """function(key, values) {
  var sum = 0;
  for(var i in values) { sum += values[i]; }
  return sum;
}""",
      output = MapReduceInlineOutput,
      query = visibleByUserQuery(userId).some)
    (for {
      row ← result.hasNext option result.next
      sum ← row.getAs[Double]("value")
    } yield sum.toInt) | 0
  }

  val all: IO[List[Thread]] = io {
    find(DBObject()).toList
  }

  def setRead(thread: Thread): IO[Unit] = io {
    for (i ← 1 to thread.nbUnread) {
      collection.update(
        selectId(thread.id) ++ DBObject("posts.isRead" -> false),
        $set("posts.$.isRead" -> true)
      )
    }
  }

  def deleteFor(user: User)(thread: Thread) = io {
    update(selectId(thread.id), $pull("visibleByUserIds" -> user.id))
  }

  def saveIO(thread: Thread): IO[Unit] = io {
    update(
      selectId(thread.id),
      _grater asDBObject thread,
      upsert = true)
  }

  def removeIO(thread: Thread): IO[Unit] = io {
    remove(DBObject("_id" -> thread.id))
  }

  def userQuery(user: User) = DBObject("userIds" -> user.id)

  def visibleByUserQuery(user: User): DBObject = 
    visibleByUserQuery(user.id)

  def visibleByUserQuery(userId: String): DBObject = 
    DBObject("visibleByUserIds" -> userId)

  def selectId(id: String) = DBObject("_id" -> id)

  val sortQuery = DBObject("updatedAt" -> -1)
}
