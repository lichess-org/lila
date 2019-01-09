package lila.user

import lila.db.dsl._
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

case class Note(
    _id: String,
    from: User.ID,
    to: User.ID,
    text: String,
    troll: Boolean,
    mod: Boolean,
    date: DateTime
) {
  def userIds = List(from, to)
  def isFrom(user: User) = user.id == from
}

case class UserNotes(user: User, notes: List[Note])

final class NoteApi(
    coll: Coll,
    timeline: akka.actor.ActorSelection,
    bus: lila.common.Bus
) {

  import reactivemongo.bson._
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val noteBSONHandler = Macros.handler[Note]

  def get(user: User, me: User, myFriendIds: Set[String], isMod: Boolean): Fu[List[Note]] =
    coll.find(
      $doc("to" -> user.id) ++
        (!me.troll ?? $doc("troll" -> false)) ++
        (if (isMod) $or(
          "from" $in (myFriendIds + me.id),
          "mod" $eq true
        )
        else
          $doc(
            "from" $in (myFriendIds + me.id),
            "mod" -> false
          ))
    ).sort($doc("date" -> -1)).list[Note](20)

  def forMod(id: User.ID): Fu[List[Note]] =
    coll.find($doc("to" -> id))
      .sort($doc("date" -> -1))
      .list[Note](20)

  def forMod(ids: List[User.ID]): Fu[List[Note]] =
    coll.find($doc("to" $in ids))
      .sort($doc("date" -> -1))
      .list[Note](50)

  def write(to: User, text: String, from: User, modOnly: Boolean) = {

    val note = Note(
      _id = ornicar.scalalib.Random nextString 8,
      from = from.id,
      to = to.id,
      text = text,
      troll = from.troll,
      mod = modOnly,
      date = DateTime.now
    )

    coll.insert(note) >>- {
      import lila.hub.actorApi.timeline.{ Propagate, NoteCreate }
      timeline ! {
        Propagate(NoteCreate(note.from, note.to)) toFriendsOf from.id exceptUser note.to modsOnly note.mod
      }
      bus.publish(lila.hub.actorApi.user.Note(
        from = from.username,
        to = to.username,
        text = note.text,
        mod = modOnly
      ), 'userNote)
    }
  } >> {
    modOnly ?? Title.fromUrl(text) flatMap {
      _ ?? { UserRepo.addTitle(to.id, _) }
    }
  }

  def byId(id: String): Fu[Option[Note]] =
    coll.byId[Note](id)

  def delete(id: String) =
    coll.remove($id(id))

  def erase(user: User) = coll.remove($doc("from" -> user.id))
}
