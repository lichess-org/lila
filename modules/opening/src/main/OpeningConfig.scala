package lila.opening

import chess.Speed
import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.RequestHeader

import lila.common.Form.{ numberIn, typeIn, given }
import lila.core.security.LilaCookie

case class OpeningConfig(ratings: Set[Int], speeds: Set[Speed]):

  override def toString = s"Speed: $showSpeeds; Rating: $showRatings"

  def isDefault = this == OpeningConfig.default

  def showRatings: String =
    showContiguous(ratings.toList.sorted.map(_.toString), OpeningConfig.contiguousRatings)(identity, identity)
  def showSpeeds: String =
    showContiguous(speeds.toList.sorted, OpeningConfig.contiguousSpeeds)(_.key.value, _.name)

  // shows contiguous rating ranges, or distinct ratings
  // 1600 to 2200
  // or 1600, 2000, 2200
  private def showContiguous[A](list: List[A], reference: String)(
      stableId: A => String,
      label: A => String
  ): String = list match
    case Nil => "All"
    case List(single) => label(single)
    case first :: rest =>
      val many = first :: rest
      val hash = many.map(stableId).mkString(",")
      if reference == hash then "All"
      else if reference.contains(hash) then s"${label(first)} to ${label(rest.lastOption | first)}"
      else many.map(label).mkString(", ")

final class OpeningConfigStore(baker: LilaCookie):
  import OpeningConfig.*

  def read(using req: RequestHeader): OpeningConfig =
    req.cookies.get(cookie.name).map(_.value).flatMap(cookie.read) | OpeningConfig.default

  def write(config: OpeningConfig) = baker.cookie(
    cookie.name,
    cookie.write(config),
    maxAge = cookie.maxAge.some,
    httpOnly = false.some
  )

object OpeningConfig:

  val allRatings = List[Int](400, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2500)
  val contiguousRatings = allRatings.mkString(",")

  val allSpeeds =
    List[Speed](
      Speed.UltraBullet,
      Speed.Bullet,
      Speed.Blitz,
      Speed.Rapid,
      Speed.Classical,
      Speed.Correspondence
    )
  val contiguousSpeeds = allSpeeds.map(_.key.value).mkString(",")

  val default = OpeningConfig(allRatings.drop(1).toSet, allSpeeds.drop(1).toSet)

  private[opening] object cookie:
    val name = "opening"
    val maxAge = 31536000 // one year
    val valueSep = '/'
    val fieldSep = '!'

    def read(str: String): Option[OpeningConfig] = str.split(fieldSep) match
      case Array(r, s) =>
        OpeningConfig(
          ratings = r.split(valueSep).flatMap(_.toIntOption).toSet,
          speeds = chess.SpeedId.from(s.split(valueSep).flatMap(_.toIntOption)).flatMap(Speed.byId.get).toSet
        ).some
      case _ => none

    def write(cfg: OpeningConfig): String = List(
      cfg.ratings.mkString(valueSep.toString),
      cfg.speeds.map(_.id).mkString(valueSep.toString)
    ).mkString(fieldSep.toString)

  val form = Form(
    mapping(
      "ratings" -> set(numberIn(allRatings)),
      "speeds" -> set(typeIn(allSpeeds.map(_.id).toSet).transform[Speed](s => Speed(s).get, _.id))
    )(OpeningConfig.apply)(lila.common.unapply)
  )

  val ratingChoices = allRatings.zip(allRatings.map(_.toString))
  val speedChoices = allSpeeds.map(_.id).zip(allSpeeds.map(_.name))
