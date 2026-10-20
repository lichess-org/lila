package lila.push

import scalalib.net.UserAgent

import scala.math.Ordered.orderingToOrdered

import lila.common.HTTPRequest
import lila.core.net.LichessMobileVersion

import LichessMobileVersion.given

final private case class Device(
    _id: String, // Firebase token
    platform: String, // cordova platform (android, ios, firebase)
    userId: UserId,
    seenAt: Instant,
    ua: UserAgent
):
  def isMobile = HTTPRequest.isLichessMobile(ua)
  def isLichobile = HTTPRequest.isLichobile(ua)

  def isMobileVersionCompatible(version: LichessMobileVersion): Boolean =
    HTTPRequest.lichessMobileVersion(ua).exists(_ >= version)

  def deviceId = platform match
    case "ios" => _id.grouped(8).mkString("<", " ", ">")
    case _ => _id
