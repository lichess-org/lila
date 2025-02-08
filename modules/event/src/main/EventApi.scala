package lila.event
import scalalib.model.Language
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class EventApi(coll: Coll, cacheApi: lila.memo.CacheApi, eventForm: EventForm)(using Executor):

  import BsonHandlers.given

  def promoteTo(accepts: Set[Language]): Fu[List[Event]] =
    promotable.getUnit.map:
      _.filter: event =>
        accepts(event.lang)
      .take(3)

  private val promotable = cacheApi.unit[List[Event]]:
    _.refreshAfterWrite(5.minutes)
      .buildAsyncFuture(_ => fetchPromotable)

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

  def list = coll.find($empty).sort($doc("startsAt" -> -1)).cursor[Event]().list(50)

  def oneEnabled(id: String) = coll.byId[Event](id).dmap(_.filter(_.enabled))

  def one(id: String) = coll.byId[Event](id)

  def editForm(event: Event) =
    eventForm.form.fill:
      EventForm.Data.make(event)

  def update(old: Event, data: EventForm.Data)(using MyId): Fu[Int] = for
    res <- coll.update.one($id(old.id), data.update(old))
    _ = promotable.invalidateUnit()
  yield res.n

  def createForm = eventForm.form

  def create(data: EventForm.Data)(using MyId): Fu[Event] =
    val event = data.make
    for
      _ <- coll.insert.one(event)
      _ = promotable.invalidateUnit()
    yield event

  def clone(old: Event) =
    old.copy(
      title = s"${old.title} (clone)",
      startsAt = nowInstant.plusDays(7)
    )
