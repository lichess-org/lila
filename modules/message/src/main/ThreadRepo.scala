package lila.message

import lila.db.api._
import lila.db.Implicits._
import tube.threadTube
import lila.common.PimpedJson._

import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import play.modules.reactivemongo.json.BSONFormats.toJSON

import scala.concurrent.Future

object ThreadRepo {

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    $find($query(userQuery(user)) sort recentSort)

  def visibleByUser(user: ID): Fu[List[Thread]] =
    $find($query(visibleByUserQuery(user)) sort recentSort)

  def userNbUnread(userId: String): Fu[Int] = {
    val command = MapReduce(
      collectionName = tube.threadTube.coll.name,
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
      query = JsObjectWriter.write(visibleByUserQuery(userId)).some)
    tube.threadTube.coll.db.command(command) map { res ⇒
      toJSON(res).arr("results").flatMap(_.apply(0) int "value")
    } map (~_)
  }

  def setRead(thread: Thread): Funit = Future sequence {
    List.fill(thread.nbUnread) {
      $update(
        $select(thread.id) ++ Json.obj("posts.isRead" -> false),
        $set("posts.$.isRead" -> true)
      )
    }
  } void

  def deleteFor(user: ID)(thread: ID) = 
    $update($select(thread), $pull("visibleByUserIds", user))

  def userQuery(user: String) = Json.obj("userIds" -> user)

  def visibleByUserQuery(user: String) = Json.obj("visibleByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
