package lila.event

import org.joda.time.{ DateTime, DateTimeZone }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo._

final class EventApi(coll: Coll) {

  import BsonHandlers._

  val promotable = AsyncCache.single(fetchPromotable, timeToLive = 5 minutes)

  def fetchPromotable: Fu[List[Event]] = coll.find($doc(
    "enabled" -> true,
    "startsAt" $gt DateTime.now.minusDays(1) $lt DateTime.now.plusDays(1)
  )).sort($doc("startsAt" -> 1)).list[Event](5).map {
    _.filter(_.isNow)
  }

  def list = coll.find($empty).sort($doc("startsAt" -> -1)).list[Event](50)

  def recentEnabled = coll
    .find($doc("enabled" -> true))
    .sort($doc("startsAt" -> -1))
    .list[Event](50)

  def one(id: String) = coll.byId[Event](id)

  def editForm(event: Event) = EventForm.form fill {
    EventForm.Data make event
  }

  def update(old: Event, data: EventForm.Data) =
    coll.update($id(old.id), data update old) >> promotable.clear

  def createForm = EventForm.form

  def create(data: EventForm.Data, userId: String): Fu[Event] = {
    val event = data make userId
    coll.insert(event) >> promotable.clear inject event
  }
}
