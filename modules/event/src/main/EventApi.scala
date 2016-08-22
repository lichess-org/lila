package lila.event

import org.joda.time.{ DateTime, DateTimeZone }

import lila.db.dsl._

final class EventApi(coll: Coll) {

  import BsonHandlers._

  def list = coll.find($empty).sort($doc("startsAt" -> -1)).list[Event](50)

  def one(id: String) = coll.byId[Event](id)

  def editForm(event: Event) = EventForm.form fill {
    EventForm.Data make event
  }

  def update(old: Event, data: EventForm.Data) =
    coll.update($id(old.id), data update old).void

  def createForm = EventForm.form

  def create(data: EventForm.Data, userId: String): Fu[Event] = {
    val event = data make userId
    coll.insert(event) inject event
  }
}
