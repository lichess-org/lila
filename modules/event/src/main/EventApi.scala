package lila.event

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi._
import lila.user.User

final class EventApi(
    coll: Coll,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def promoteTo(req: RequestHeader): Fu[List[Event]] =
    promotable.getUnit map {
      _.filter { event =>
        event.lang.language == lila.i18n.enLang.language ||
        lila.i18n.I18nLangPicker.allFromRequestHeaders(req).exists {
          _.language == event.lang.language
        }
      }
    }

  private val promotable = cacheApi.unit[List[Event]] {
    _.refreshAfterWrite(5 minutes)
      .buildAsyncFuture(_ => fetchPromotable)
  }

  def fetchPromotable: Fu[List[Event]] =
    coll
      .find(
        $doc(
          "enabled" -> true,
          "startsAt" $gt DateTime.now.minusDays(1) $lt DateTime.now.plusDays(1)
        )
      )
      .sort($sort asc "startsAt")
      .cursor[Event]()
      .list(10)
      .dmap {
        _.filter(_.featureNow) take 3
      }

  def list = coll.find($empty).sort($doc("startsAt" -> -1)).cursor[Event]().list(50)

  def oneEnabled(id: String) = coll.byId[Event](id).map(_.filter(_.enabled))

  def one(id: String) = coll.byId[Event](id)

  def editForm(event: Event) =
    EventForm.form fill {
      EventForm.Data make event
    }

  def update(old: Event, data: EventForm.Data, by: User): Fu[Int] =
    (coll.update.one($id(old.id), data.update(old, by)) >>- promotable.invalidateUnit()).map(_.n)

  def createForm = EventForm.form

  def create(data: EventForm.Data, userId: String): Fu[Event] = {
    val event = data make userId
    coll.insert.one(event) >>- promotable.invalidateUnit() inject event
  }

  def clone(old: Event) =
    old.copy(
      title = s"${old.title} (clone)",
      startsAt = DateTime.now plusDays 7
    )
}
