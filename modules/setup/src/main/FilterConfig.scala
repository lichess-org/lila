package lila.setup

import chess.{ Variant, Mode, Speed }
import lila.rating.RatingRange

case class FilterConfig(
    variant: List[Variant],
    mode: List[Mode],
    speed: List[FilterConfig.SpeedOrCorrespondence],
    ratingRange: RatingRange) {

  def >> = (
    variant map (_.id),
    mode map (_.id),
    speed map FilterConfig.speedId,
    ratingRange.toString
  ).some

  def render = play.api.libs.json.Json.obj(
    "variant" -> variant.map(_.shortName),
    "mode" -> mode.map(_.toString),
    "speed" -> speed.map(FilterConfig.speedId),
    "rating" -> ratingRange.notBroad.map(rr => List(rr.min, rr.max)))

  def nonEmpty = copy(
    variant = variant.isEmpty.fold(FilterConfig.default.variant, variant),
    mode = mode.isEmpty.fold(FilterConfig.default.mode, mode),
    speed = speed.isEmpty.fold(FilterConfig.default.speed, speed))
}

object FilterConfig {

  type SpeedOrCorrespondence = Option[Speed]

  def speedId(speed: SpeedOrCorrespondence) = speed.fold(Config.correspondenceSpeedId)(_.id)

  val variants = List(Variant.Standard, Variant.Chess960, Variant.KingOfTheHill, Variant.ThreeCheck)
  val modes = Mode.all
  val speeds: List[SpeedOrCorrespondence] = Speed.all.map(_.some) :+ None

  val default = FilterConfig(
    variant = variants,
    mode = modes,
    speed = speeds,
    ratingRange = RatingRange.default)

  def <<(v: List[Int], m: List[Int], s: List[Int], e: String) = new FilterConfig(
    variant = v flatMap Variant.apply,
    mode = m flatMap { Mode(_) },
    speed = s map Speed.apply,
    ratingRange = RatingRange orDefault e
  ).nonEmpty

  import reactivemongo.bson._
  import lila.db.BSON

  private[setup] implicit val filterConfigBSONHandler = new BSON[FilterConfig] {

    def reads(r: BSON.Reader): FilterConfig = FilterConfig(
      variant = r intsD "v" flatMap Variant.apply,
      mode = r intsD "m" flatMap { Mode(_) },
      speed = r intsD "s" map { Speed(_) },
      ratingRange = r strO "e" flatMap RatingRange.apply getOrElse RatingRange.default)

    def writes(w: BSON.Writer, o: FilterConfig) = BSONDocument(
      "v" -> o.variant.map(_.id),
      "m" -> o.mode.map(_.id),
      "s" -> o.speed.map(speedId),
      "e" -> o.ratingRange.toString)
  }

  private[setup] val tube = lila.db.BsTube(filterConfigBSONHandler)
}
