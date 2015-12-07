package lila.push

import org.joda.time.DateTime

case class Device(
  _id: String, // auto-generated
  userId: String,
  deviceId: String,
  date: DateTime)
