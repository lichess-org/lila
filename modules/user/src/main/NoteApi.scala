package lila.user

import scalalib.ThreadLocalRandom
import scalalib.paginator.Paginator

import lila.db.dsl.{ *, given }
import lila.core.perm.Granter

case class Note(
    _id: String,
    from: UserId,
    to: UserId,
    text: String,
    mod: Boolean,
    dox: Boolean,
    date: Instant
) extends lila.core.user.Note:
  def userIds = List(from, to)
  def isFrom(user: User) = user.id.is(from)
  def searchable = mod && from.isnt(UserId.lichess) && from.isnt(UserId.watcherbot) &&
    !text.startsWith("Appeal reply:")

final class NoteApi(coll: Coll)(using Executor) extends lila.core.user.NoteApi:

  import reactivemongo.api.bson.*
  private given bsonHandler: BSONDocumentHandler[Note] = Macros.handler[Note]

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    for
      _ <- coll.delete.one($doc("from" -> del.id, "mod" -> false)) // hits the from_1 partial index
      maybeKeepModNotes = del.user.marks.dirty.so($doc("mod" -> false))
      _ <- coll.delete.one($doc("to" -> del.id) ++ maybeKeepModNotes)
    yield ()

  def getForMyPermissions(user: User, max: Max = Max(30))(using me: Me): Fu[List[Note]] =
    coll
      .find(
        $doc("to" -> user.id) ++ {
          if Granter(_.ModNote) then
            $or(
              $doc("from" -> me.userId),
              $doc("mod" -> true)
            )
          else $doc("from" -> me.userId, "mod" -> false)
        } ++
          (!Granter(_.Admin)).so($doc("dox" -> false))
      )
      .sort($sort.desc("date"))
      .cursor[Note]()
      .list(max.value)

  def toUserForMod(id: UserId, max: Max = Max(50)): Fu[List[Note]] =
    coll
      .find($doc("to" -> id, "mod" -> true))
      .sort($sort.desc("date"))
      .cursor[Note]()
      .list(max.value)

  def recentToUserForMod(id: UserId): Fu[Option[Note]] =
    toUserForMod(id, Max(1))
      .map(_.headOption.filter(_.date.isAfter(nowInstant.minusMinutes(5))))

  def byUsersForMod(ids: List[UserId]): Fu[List[Note]] =
    coll
      .find($doc("to".$in(ids), "mod" -> true))
      .sort($sort.desc("date"))
      .cursor[Note]()
      .list(100)

  def write(to: UserId, text: String, modOnly: Boolean, dox: Boolean)(using me: MyId): Funit =
    val note = Note(
      _id = ThreadLocalRandom.nextString(8),
      from = me,
      to = to,
      text = text,
      mod = modOnly,
      dox = modOnly && dox,
      date = nowInstant
    )
    Future
      .fromTry(bsonHandler.writeTry(note))
      .flatMap: base =>
        val bson = if note.searchable then base ++ searchableBsonFlag else base
        coll.insert.one(bson).void

  def lichessWrite(to: User, text: String) =
    write(to.id, text, modOnly = true, dox = false)(using UserId.lichessAsMe)

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
              Match(selector) -> List(Sort(Descending("date")), Skip(offset), Limit(length))
            .map:
              _.flatMap:
                _.asOpt[Note]
      ,
      currentPage = page,
      maxPerPage = MaxPerPage(30)
    )
