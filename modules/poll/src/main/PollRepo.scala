package lila.poll

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import reactivemongo.api.bson._

final private class PollRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  // import BSONHandlers._
  implicit val PollBSONHandler = Macros.handler[Poll]

  def insert(poll: Poll) =
    coll.insert.one(poll).void

  def markVote(pid: Poll.ID, uid: User.ID, choice: Int) = {}

  def close(pid: Poll.ID): Fu[Poll] = {
    coll.update.one(
      $id(pid),
      $set("isClosed" -> true),
      multi = false
    ) map { poll =>
    }
  }
}

//def remove(notifies: Notification.Notifies, selector: Bdoc): Funit =
//coll.delete.one(userNotificationsQuery(notifies) ++ selector).void
