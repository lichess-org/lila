package lila.relay

import org.joda.time.DateTime

case class Content(
  _id: String,
  short: Option[String],
  long: Option[String],
  notes: Option[String],
  createdAt: DateTime,
  updatedAt: DateTime,
  updatedBy: String)

object Content {

  def mkId(relay: Relay) = relay.baseSlug
}
