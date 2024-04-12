package lila.push

import lila.core.net.UserAgent

final private case class Device(
    _id: String,      // Firebase token
    platform: String, // cordova platform (android, ios, firebase)
    userId: UserId,
    seenAt: Instant,
    ua: Option[UserAgent]
):
  def isMobile = ua.exists(lila.common.HTTPRequest.isLichessMobile)

  def deviceId = platform match
    case "ios" => _id.grouped(8).mkString("<", " ", ">")
    case _     => _id
