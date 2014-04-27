package lila.user

import org.joda.time.DateTime

case class Note(
  _id: String,
  from: String,
  to: String,
  text: String,
  troll: Boolean,
  date: DateTime)

final class NoteApi(
    coll: lila.db.Types.Coll,
    timeline: akka.actor.ActorSelection) {

  import reactivemongo.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val noteBSONHandler = Macros.handler[Note]

  def get(user: User, me: User, myFriendIds: Set[String]): Fu[List[Note]] =
    coll.find(
      BSONDocument(
        "to" -> user.id,
        "from" -> BSONDocument("$in" -> (myFriendIds + me.id))
      ) ++ me.troll.fold(BSONDocument(), BSONDocument("troll" -> false))
    ).sort(BSONDocument("date" -> -1)).cursor[Note].collect[List](100)

  def write(to: User, text: String, from: User) = {

    val note = Note(
      _id = ornicar.scalalib.Random nextStringUppercase 8,
      from = from.id,
      to = to.id,
      text = text,
      troll = from.troll,
      date = DateTime.now)

    import lila.hub.actorApi.timeline.{ Propagate, NoteCreate }

    timeline ! (Propagate(NoteCreate(note.from, note.to)) toFriendsOf from.id)

    coll insert note
  }
}
