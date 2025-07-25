package lila.push

import scala.math.Ordered.orderingToOrdered

import lila.core.net.{ UserAgent, LichessMobileVersion }
import LichessMobileVersion.given

final private case class Device(
    _id: String, // Firebase token
    platform: String, // cordova platform (android, ios, firebase)
    userId: UserId,
    seenAt: Instant,
    ua: Option[UserAgent]
):
  def isMobile = ua.exists(lila.common.HTTPRequest.isLichessMobile)

  def isMobileVersionCompatible(version: LichessMobileVersion): Boolean =
    ua.exists: devUa =>
      lila.common.HTTPRequest
        .lichessMobileVersion(devUa)
        .exists:
          _ >= version

  def deviceId = platform match
    case "ios" => _id.grouped(8).mkString("<", " ", ">")
    case _ => _id
