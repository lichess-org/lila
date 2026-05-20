package lila.event
import scalalib.model.Language
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import scalalib.paginator.Paginator

final class EventApi(coll: Coll, cacheApi: lila.memo.CacheApi, eventForm: EventForm, ircApi: lila.irc.IrcApi)(
    using
    Executor,
    Scheduler
):

  import BsonHandlers.given

  def promoteTo(accepts: Set[Language]): Fu[List[Event]] =
    promotable.getUnit.map:
      _.filter: event =>
        accepts(event.lang)
      .take(3)

  private val promotable = cacheApi.unit[List[Event]]:
    _.refreshAfterWrite(5.minutes).buildAsyncTimeout()(_ => fetchPromotable)

  def fetchPromotable: Fu[List[Event]] =
    coll
      .find:
        $doc(
          "enabled" -> true,
          "startsAt".$gt(nowInstant.minusDays(1)).$lt(nowInstant.plusDays(1))
        )
      .sort($sort.asc("startsAt"))
      .cursor[Event]()
      .list(50)
      .dmap:
        _.filter(_.featureNow).take(10)

  def pager(page: Int) = Paginator(
    adapter = lila.db.paginator.Adapter[Event](
      collection = coll,
      selector = $empty,
      projection = none,
      sort = $sort.desc("startsAt"),
      _.sec
    ),
    currentPage = page,
    maxPerPage = MaxPerPage(40)
  )

  def oneEnabled(id: String) = coll.byId[Event](id).dmap(_.filter(_.enabled))

  def one(id: String) = coll.byId[Event](id)

  def editForm(event: Event) =
    eventForm.form.fill:
      EventForm.Data.make(event)

  def update(old: Event, data: EventForm.Data)(using MyId): Fu[Int] =
    val next = data.update(old)
    for
      res <- coll.update.one($id(old.id), next)
      _ = promotable.invalidateUnit()
      _ = notifyBBB(next, old.some)
    yield res.n

  def createForm = eventForm.form

  def create(data: EventForm.Data)(using MyId): Fu[Event] =
    val event = data.make
    for
      _ <- coll.insert.one(event)
      _ = promotable.invalidateUnit()
      _ = notifyBBB(event, none)
    yield event

  def clone(old: Event) =
    old.copy(
      title = s"${old.title} (clone)",
      startsAt = nowInstant.plusDays(7)
    )

  private def notifyBBB(next: Event, prev: Option[Event])(using me: MyId) =
    if prev.map(_.featureDates).forall(_ != next.featureDates) then
      ircApi.bbb(
        me,
        "event",
        next.title,
        routes.Event.show(next.id),
        next.featureSince,
        next.featureUntil.some
      )
