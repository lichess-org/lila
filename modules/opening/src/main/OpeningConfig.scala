package lila.opening

import chess.Speed
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.RequestHeader

import lila.common.Form.numberIn
import lila.common.Iso
import lila.common.LilaCookie

final class OpeningConfigStore(baker: LilaCookie) {
  import OpeningConfig._

  def read(implicit req: RequestHeader): OpeningConfig =
    req.cookies.get(cookie.name).map(_.value).flatMap(cookie.read) | OpeningConfig.default

  def write(config: OpeningConfig)(implicit req: RequestHeader) = baker.cookie(
    cookie.name,
    cookie.write(config),
    maxAge = cookie.maxAge.some,
    httpOnly = false.some
  )
}

case class OpeningConfig(ratings: Set[Int], speeds: Set[Speed])

object OpeningConfig {

  val allRatings = List[Int](1600, 1800, 2000, 2200, 2500)
  val allSpeeds =
    List[Speed](
      Speed.UltraBullet,
      Speed.Bullet,
      Speed.Blitz,
      Speed.Rapid,
      Speed.Classical,
      Speed.Correspondence
    )

  val default = OpeningConfig(allRatings.drop(1).toSet, allSpeeds.drop(1).toSet)

  private[opening] object cookie {
    val name     = "opening"
    val maxAge   = 31536000 // one year
    val valueSep = '/'
    val fieldSep = '!'

    def read(str: String): Option[OpeningConfig] = str split fieldSep match {
      case Array(r, s) =>
        OpeningConfig(
          ratings = r.split(valueSep).flatMap(_.toIntOption).toSet,
          speeds = s.split(valueSep).flatMap(_.toIntOption).flatMap(Speed.byId.get).toSet
        ).some
      case _ => none
    }

    def write(cfg: OpeningConfig): String = List(
      cfg.ratings.mkString(valueSep.toString),
      cfg.speeds.map(_.id).mkString(valueSep.toString)
    ) mkString fieldSep.toString
  }

  val form = Form(
    mapping(
      "ratings" -> set(numberIn(allRatings)),
      "speeds"  -> set(numberIn(allSpeeds.map(_.id)).transform[Speed](s => Speed(s).get, _.id))
    )(OpeningConfig.apply)(OpeningConfig.unapply)
  )

  val ratingChoices = allRatings zip allRatings.map(_.toString)
  val speedChoices  = allSpeeds.map(_.id) zip allSpeeds.map(_.name)
}
