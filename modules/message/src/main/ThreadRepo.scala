package lila.message

import scala.concurrent.Future

import play.api.libs.json.Json

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import tube.threadTube

object ThreadRepo {
import play.modules.reactivemongo.json._

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    $find($query(userQuery(user)) sort recentSort)

  def visibleByUser(user: ID): Fu[List[Thread]] =
    $find($query(visibleByUserQuery(user)) sort recentSort)

  def visibleByUser(user: ID, nb: Int): Fu[List[Thread]] =
    $find($query(visibleByUserQuery(user)) sort recentSort, nb)

  def userUnreadIds(userId: String): Fu[List[String]] = {
    import BSONMapReduceCommandImplicits._

    val command = BSONMapReduceCommand.MapReduce(
      mapFunction = """function() {
  var thread = this;
  thread.posts.forEach(function(p) {
    if (!p.isRead) {
      if (thread.creatorId == "%s") {
        if (!p.isByCreator) emit("i", thread._id);
      } else if (p.isByCreator) emit("i", thread._id);
    }
  });
  }""" format userId,
      reduceFunction = """function(key, values) {
  var ids = [];
  for(var i in values) { ids.push(values[i]); }
  return ids.join(';');
  }""",
      query = JsObjectWriter.write(
        visibleByUserQuery(userId) ++ Json.obj("posts.isRead" -> false)
      ).some,
      sort = JsObjectWriter.write(Json.obj("updatedAt" -> -1)).some)

    tube.threadTube.coll.runCommand(command) map { res =>
      BSONFormats.toJSON(res).arr("results").flatMap(_.apply(0).toOption).flatMap(_ str "value")
    } map {
      _ ?? (_ split ';' toList)
    }
  }

  def setRead(thread: Thread): Funit = {
    List.fill(thread.nbUnread) {
      $update(
        $select(thread.id) ++ Json.obj("posts.isRead" -> false),
        $set("posts.$.isRead" -> true)
      )
    }
  }.sequenceFu.void

  def deleteFor(user: ID)(thread: ID) =
    $update($select(thread), $pull("visibleByUserIds", user))

  def reallyDeleteByCreatorId(user: ID) = $remove(Json.obj("creatorId" -> user))

  def visibleByUserContainingExists(user: ID, containing: String): Fu[Boolean] =
    $count.exists(visibleByUserQuery(user) ++ Json.obj(
      "posts.0.text" -> $regex(containing)))

  def userQuery(user: String) = Json.obj("userIds" -> user)

  def visibleByUserQuery(user: String) = Json.obj("visibleByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
