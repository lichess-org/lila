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

case class OpeningConfig(ratings: Set[Int])

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

  val default = OpeningConfig(allRatings.drop(1).toSet) // allSpeeds.toSet excl Speed.UltraBullet)

  private[opening] object cookie {
    val name     = "opening"
    val maxAge   = 31536000 // one year
    val valueSep = '/'

    def read(str: String): Option[OpeningConfig] =
      OpeningConfig(
        ratings = str.split(valueSep).flatMap(_.toIntOption).toSet
      ).some

    def write(cfg: OpeningConfig): String =
      cfg.ratings.mkString(valueSep.toString)
  }

  val form = Form(
    mapping(
      "ratings" -> set(numberIn(allRatings))
      // "speed"     -> numberIn(OpeningQuery.allRatings),
    )(OpeningConfig.apply)(OpeningConfig.unapply)
  )

  val ratingChoices = allRatings zip allRatings.map(_.toString)
}
