package lila.user

import lila.db.dsl.{ *, given }
import ornicar.scalalib.ThreadLocalRandom

case class Note(
    _id: String,
    from: UserId,
    to: UserId,
    text: String,
    mod: Boolean,
    dox: Boolean,
    date: Instant
):
  def userIds            = List(from, to)
  def isFrom(user: User) = user.id == from

final class NoteApi(
    userRepo: UserRepo,
    coll: Coll
)(using
    ec: Executor,
    ws: play.api.libs.ws.StandaloneWSClient
):

  import reactivemongo.api.bson.*
  private given BSONDocumentHandler[Note] = Macros.handler[Note]

  def get(user: User, me: User, isMod: Boolean): Fu[List[Note]] =
    coll
      .find(
        $doc("to" -> user.id) ++ {
          if (isMod)
            $or(
              $doc("from" -> me.id),
              $doc("mod"  -> true)
            )
          else
            $doc("from" -> me.id, "mod" -> false)
        }
      )
      .sort($sort desc "date")
      .cursor[Note]()
      .list(20)

  def byUserForMod(id: UserId): Fu[List[Note]] =
    coll
      .find($doc("to" -> id, "mod" -> true))
      .sort($sort desc "date")
      .cursor[Note]()
      .list(50)

  def byUsersForMod(ids: List[UserId]): Fu[List[Note]] =
    coll
      .find($doc("to" $in ids, "mod" -> true))
      .sort($sort desc "date")
      .cursor[Note]()
      .list(100)

  def write(to: User, text: String, from: User, modOnly: Boolean, dox: Boolean) = {

    val note = Note(
      _id = ThreadLocalRandom nextString 8,
      from = from.id,
      to = to.id,
      text = text,
      mod = modOnly,
      dox = modOnly && (dox || Title.fromUrl.toFideId(text).isDefined),
      date = nowInstant
    )

    coll.insert.one(note) >>-
      lila.common.Bus.publish(
        lila.hub.actorApi.user.Note(
          from = from.username,
          to = to.username,
          text = note.text,
          mod = modOnly
        ),
        "userNote"
      )
  } >> {
    modOnly ?? Title.fromUrl(text) flatMap {
      _ ?? { userRepo.addTitle(to.id, _) }
    }
  }

  def lichessWrite(to: User, text: String) =
    userRepo.lichess flatMapz {
      write(to, text, _, modOnly = true, dox = false)
    }

  def byId(id: String): Fu[Option[Note]] = coll.byId[Note](id)

  def delete(id: String) = coll.delete.one($id(id))

  def setDox(id: String, dox: Boolean) = coll.update.one($id(id), $set("dox" -> dox)).void
