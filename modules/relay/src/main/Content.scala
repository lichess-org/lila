package lila.relay

import org.joda.time.DateTime

case class Content(
    _id: String,
    short: Option[String],
    long: Option[String],
    notes: Option[String],
    updatedAt: DateTime,
    updatedBy: String) {

  def id = _id

  def matches(relay: Relay) = id == relay.baseSlug
}

object Content {

  def mkId(relay: Relay) = relay.baseSlug
}
