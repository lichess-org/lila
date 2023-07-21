package lila.user

import lila.db.dsl.{ *, given }
import ornicar.scalalib.ThreadLocalRandom
import lila.common.paginator.Paginator
import lila.common.config.MaxPerPage

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
  def isFrom(user: User) = user.id is from
  def searchable = mod && from.isnt(User.lichessId) && from.isnt(User.watcherbotId) &&
    !text.startsWith("Appeal reply:")

final class NoteApi(userRepo: UserRepo, coll: Coll)(using
    Executor,
    play.api.libs.ws.StandaloneWSClient
):

  import reactivemongo.api.bson.*
  private given bsonHandler: BSONDocumentHandler[Note] = Macros.handler[Note]

  def get(user: User, isMod: Boolean)(using me: Me.Id): Fu[List[Note]] =
    coll
      .find(
        $doc("to" -> user.id) ++ {
          if isMod then
            $or(
              $doc("from" -> me),
              $doc("mod"  -> true)
            )
          else $doc("from" -> me, "mod" -> false)
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

  def write(to: User, text: String, modOnly: Boolean, dox: Boolean)(using me: Me) = {
    val note = Note(
      _id = ThreadLocalRandom nextString 8,
      from = me,
      to = to.id,
      text = text,
      mod = modOnly,
      dox = modOnly && (dox || Title.fromUrl.toFideId(text).isDefined),
      date = nowInstant
    )
    Future
      .fromTry(bsonHandler.writeTry(note))
      .flatMap: base =>
        val bson = if note.searchable then base ++ searchableBsonFlag else base
        coll.insert.one(bson)
  } >> {
    modOnly so Title.fromUrl(text) flatMap {
      _ so { userRepo.addTitle(to.id, _) }
    }
  }

  def lichessWrite(to: User, text: String) =
    userRepo.lichess.flatMapz: lichess =>
      write(to, text, modOnly = true, dox = false)(using Me(lichess))

  def byId(id: String): Fu[Option[Note]] = coll.byId[Note](id)

  def delete(id: String) = coll.delete.one($id(id))

  def setDox(id: String, dox: Boolean) = coll.updateField($id(id), "dox", dox).void

  private val searchableBsonFlag = $doc("s" -> true)

  def search(query: String, page: Int, withDox: Boolean): Fu[Paginator[Note]] =
    Paginator(
      adapter = new:
        private val selector =
          val base = searchableBsonFlag ++ (!withDox).so($doc("dox" -> false))
          if query.nonEmpty
          then base ++ $text(query)
          else base
        def nbResults: Fu[Int] =
          if query.nonEmpty
          then coll.countSel(selector)
          else fuccess(500_000)
        def slice(offset: Int, length: Int): Fu[List[Note]] =
          coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selector) -> {
                List(Sort(Descending("date"))) :::
                  List(Skip(offset), Limit(length))
              }
            .map:
              _.flatMap:
                _.asOpt[Note]
      ,
      currentPage = page,
      maxPerPage = MaxPerPage(30)
    )
