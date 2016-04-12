package lila.user

import lila.db.dsl._
import org.joda.time.DateTime

case class Note(
  _id: String,
  from: String,
  to: String,
  text: String,
  troll: Boolean,
  date: DateTime)

final class NoteApi(
    coll: Coll,
    timeline: akka.actor.ActorSelection) {

  import reactivemongo.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val noteBSONHandler = Macros.handler[Note]

  def get(user: User, me: User, myFriendIds: Set[String]): Fu[List[Note]] =
    coll.find(
      $doc(
        "to" -> user.id,
        "from" -> $doc("$in" -> (myFriendIds + me.id))
      ) ++ me.troll.fold($doc(), $doc("troll" -> false))
    ).sort($doc("date" -> -1)).cursor[Note]().gather[List](100)

  def write(to: User, text: String, from: User) = {

    val note = Note(
      _id = ornicar.scalalib.Random nextStringUppercase 8,
      from = from.id,
      to = to.id,
      text = text,
      troll = from.troll,
      date = DateTime.now)

    import lila.hub.actorApi.timeline.{ Propagate, NoteCreate }
    timeline ! (Propagate(NoteCreate(note.from, note.to)) toFriendsOf from.id exceptUser note.to)

    coll insert note
  }
}
