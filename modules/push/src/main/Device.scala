package lila.push

import org.joda.time.DateTime

private final case class Device(
    _id: String, // google device ID
    userId: String,
    seenAt: DateTime) {

  def id = _id
}
