package lila.user

import scalalib.ThreadLocalRandom
import scalalib.paginator.Paginator

import lila.db.dsl.{ *, given }

case class Note(
    _id: String,
    from: UserId,
    to: UserId,
    text: String,
    mod: Boolean,
    dox: Boolean,
    date: Instant
) extends lila.core.user.Note:
  def userIds            = List(from, to)
  def isFrom(user: User) = user.id.is(from)
  def searchable = mod && from.isnt(UserId.lichess) && from.isnt(UserId.watcherbot) &&
    !text.startsWith("Appeal reply:")

final class NoteApi(userRepo: UserRepo, coll: Coll)(using Executor) extends lila.core.user.NoteApi:

  import reactivemongo.api.bson.*
  private given bsonHandler: BSONDocumentHandler[Note] = Macros.handler[Note]

  def get(user: User, isMod: Boolean)(using me: MyId): Fu[List[Note]] =
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
      .sort($sort.desc("date"))
      .cursor[Note]()
      .list(20)

  def byUserForMod(id: UserId, max: Max = Max(50)): Fu[List[Note]] =
    coll
      .find($doc("to" -> id, "mod" -> true))
      .sort($sort.desc("date"))
      .cursor[Note]()
      .list(max.value)

  def recentByUserForMod(id: UserId): Fu[Option[Note]] =
    byUserForMod(id, Max(1))
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
