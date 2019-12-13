package lila.push

import org.joda.time.DateTime

final private case class Device(
    _id: String,      // Firebase token or OneSignal playerId
    platform: String, // cordova platform (android, ios)
    userId: String,
    seenAt: DateTime
) {

  def deviceId = platform match {
    case "ios" => _id.grouped(8).mkString("<", " ", ">")
    case _     => _id
  }
}
