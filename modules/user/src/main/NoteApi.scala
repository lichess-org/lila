package lila.user

import lila.db.dsl._
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

case class Note(
  _id: String,
  from: String,
  to: String,
  text: String,
  troll: Boolean,
  mod: Boolean,
  date: DateTime)

final class NoteApi(
    coll: Coll,
    timeline: akka.actor.ActorSelection) {

  import reactivemongo.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val noteBSONHandler = Macros.handler[Note]

  def get(user: User, me: User, myFriendIds: Set[String], isMod: Boolean): Fu[List[Note]] =
    coll.find(
      $doc("to" -> user.id) ++
        me.troll.fold($empty, $doc("troll" -> false)) ++
        isMod.fold(
          $or(
            "from" $in (myFriendIds + me.id),
            "mod" $eq true
          ),
          $doc(
            "from" $in (myFriendIds + me.id),
            "mod" -> false
          )
        )
    ).sort($doc("date" -> -1)).cursor[Note]().gather[List](20)

  def byUserIdsForMod(ids: List[User.ID]): Fu[List[Note]] =
    coll.find($doc(
      "to" $in ids,
      "mod" -> true
    )).sort($doc("date" -> -1))
      .cursor[Note](readPreference = ReadPreference.secondaryPreferred)
      .gather[List](ids.size * 5)

  def write(to: User, text: String, from: User, modOnly: Boolean) = {

    val note = Note(
      _id = ornicar.scalalib.Random nextStringUppercase 8,
      from = from.id,
      to = to.id,
      text = text,
      troll = from.troll,
      mod = modOnly,
      date = DateTime.now)

    import lila.hub.actorApi.timeline.{ Propagate, NoteCreate }
    timeline ! {
      Propagate(NoteCreate(note.from, note.to)) toFriendsOf from.id exceptUser note.to modsOnly note.mod
    }

    coll insert note
  }
}
