package lila.relay

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._

final class ContentApi(coll: Coll) {

  private def selectId(id: String) = BSONDocument("_id" -> id)

  def byId(id: String): Fu[Option[Content]] = coll.find(selectId(id)).one[Content]

  def byRelay(relay: Relay): Fu[Option[Content]] = byId(Content mkId relay)

  def byIds(ids: Seq[String]): Fu[List[Content]] = coll.find(
    BSONDocument("_id" -> BSONDocument("$in" -> ids))
  ).cursor[Content].collect[List]()

  def byRelays(relays: Seq[Relay]): Fu[List[Content]] = byIds(relays.map(Content.mkId).distinct)

  def upsert(relay: Relay, data: ContentApi.Data, user: lila.user.User): Funit = {
    val now = DateTime.now
    byRelay(relay) flatMap {
      case None => coll.insert(Content(
        _id = Content mkId relay,
        short = data.short,
        long = data.long,
        notes = data.notes,
        updatedAt = now,
        updatedBy = user.id))
      case Some(content) => coll.update(
        selectId(Content mkId relay),
        content.copy(
          short = data.short,
          long = data.long,
          notes = data.notes,
          updatedAt = now,
          updatedBy = user.id))
    } void
  }
}

object ContentApi {

  import play.api.data._
  import play.api.data.Forms._
  import play.api.data.validation.Constraints._

  val form: Form[Data] = Form(mapping(
    "short" -> optional(nonEmptyText(maxLength = 140)),
    "long" -> optional(nonEmptyText(maxLength = 3000)),
    "notes" -> optional(nonEmptyText(maxLength = 3000))
  )(Data.apply)(Data.unapply))

  def form(content: Option[Content]): Form[Data] = content.fold(form) { c =>
    form fill Data(
      short = c.short,
      long = c.long,
      notes = c.notes)
  }

  case class Data(
    short: Option[String],
    long: Option[String],
    notes: Option[String])
}
