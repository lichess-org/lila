package lila.push

import org.joda.time.DateTime

private final case class Device(
    _id: String, // google device ID
    platform: String, // cordova platform (android, ios)
    userId: String,
    seenAt: DateTime) {

  def id = _id
}
