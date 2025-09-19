package lila.push

import scala.math.Ordered.orderingToOrdered

import lila.core.net.{ UserAgent, LichessMobileVersion }
import LichessMobileVersion.given

final private case class Device(
    _id: String, // Firebase token
    platform: String, // cordova platform (android, ios, firebase)
    userId: UserId,
    seenAt: Instant,
    ua: UserAgent
):
  def isMobile = lila.common.HTTPRequest.isLichessMobile(ua)

  def isMobileVersionCompatible(version: LichessMobileVersion): Boolean =
    lila.common.HTTPRequest.lichessMobileVersion(ua).exists(_ >= version)

  def deviceId = platform match
    case "ios" => _id.grouped(8).mkString("<", " ", ">")
    case _ => _id
