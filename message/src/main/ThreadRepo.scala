package lila.message

import lila.user.User

import lila.db.{ Repo, DbApi }
import lila.db.Implicits._

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import play.modules.reactivemongo.Implicits._
import play.modules.reactivemongo.MongoJSONHelpers._

import org.joda.time.DateTime
import ornicar.scalalib.Random

import scala.concurrent.Future

final class ThreadRepo(coll: ReactiveColl) extends Repo[Thread](coll, Threads.json) {

  def byUser(user: ID): Fu[List[Thread]] =
    find(query(userQuery(user)) sort recentSort)

  def visibleByUser(user: ID): Fu[List[Thread]] =
    find(query(visibleByUserQuery(user)) sort recentSort)

  // def userNbUnread(userId: String): IO[Int] = io {
  //   val result = collection.mapReduce(
  //     mapFunction = """function() {
  // var thread = this, nb = 0;
  // thread.posts.forEach(function(p) {
  //   if (!p.isRead) {
  //     if (thread.creatorId == "%s") {
  //       if (!p.isByCreator) nb++;
  //     } else if (p.isByCreator) nb++;
  //   }
  // });
  // if (nb > 0) emit("n", nb);
  // }""" format userId,
  //     reduceFunction = """function(key, values) {
  // var sum = 0;
  // for(var i in values) { sum += values[i]; }
  // return sum;
  // }""",
  //     output = MapReduceInlineOutput,
  //     query = visibleByUserQuery(userId).some)
  //   (for {
  //     row ← result.hasNext option result.next
  //     sum ← row.getAs[Double]("value")
  //   } yield sum.toInt) | 0
  // }

  def setRead(thread: Thread): Funit =
    Future.sequence(1 to thread.nbUnread map { _ ⇒
      update(
        select(thread.id) ++ Json.obj("posts.isRead" -> false),
        $set("posts.$.isRead" -> true)
      )
    }).void

  def deleteFor(user: ID)(thread: ID) = 
    update(select(thread), $pull("visibleByUserIds", user))

  def userQuery(user: String) = Json.obj("userIds" -> user)

  def visibleByUserQuery(user: String) = Json.obj("visibleByUserIds" -> user)

  val recentSort = sort desc "updatedAt"
}
