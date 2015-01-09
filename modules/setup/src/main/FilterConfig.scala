package lila.setup

import chess.{ Variant, Mode, Speed }
import lila.rating.RatingRange

case class FilterConfig(
    variant: List[Variant],
    mode: List[Mode],
    speed: List[Speed],
    ratingRange: RatingRange) {

  def >> = (
    variant map (_.id),
    mode map (_.id),
    speed map (_.id),
    ratingRange.toString
  ).some

  def render = play.api.libs.json.Json.obj(
    "variant" -> variant.map(_.shortName),
    "mode" -> mode.map(_.id),
    "speed" -> speed.map(_.id),
    "rating" -> ratingRange.notBroad.map(rr => List(rr.min, rr.max)))

  def nonEmpty = copy(
    variant = variant.isEmpty.fold(FilterConfig.default.variant, variant),
    mode = mode.isEmpty.fold(FilterConfig.default.mode, mode),
    speed = speed.isEmpty.fold(FilterConfig.default.speed, speed))
}

object FilterConfig {

  val variants = List(Variant.Standard, Variant.Chess960, Variant.KingOfTheHill, Variant.ThreeCheck, Variant.Antichess, Variant.Atomic)
  val modes = Mode.all
  val speeds = Speed.all

  val default = FilterConfig(
    variant = variants,
    mode = modes,
    speed = speeds,
    ratingRange = RatingRange.default)

  def <<(v: List[Int], m: List[Int], s: List[Int], e: String) = new FilterConfig(
    variant = v map Variant.apply flatten,
    mode = m map Mode.apply flatten,
    speed = s map Speed.apply flatten,
    ratingRange = RatingRange orDefault e
  ).nonEmpty

  import reactivemongo.bson._
  import lila.db.BSON

  private[setup] implicit val filterConfigBSONHandler = new BSON[FilterConfig] {

    def reads(r: BSON.Reader): FilterConfig = FilterConfig(
      variant = r intsD "v" flatMap Variant.apply,
      mode = r intsD "m" flatMap { Mode(_) },
      speed = r intsD "s" flatMap { Speed(_) },
      ratingRange = r strO "e" flatMap RatingRange.apply getOrElse RatingRange.default)

    def writes(w: BSON.Writer, o: FilterConfig) = BSONDocument(
      "v" -> o.variant.map(_.id),
      "m" -> o.mode.map(_.id),
      "s" -> o.speed.map(_.id),
      "e" -> o.ratingRange.toString)
  }

  private[setup] val tube = lila.db.BsTube(filterConfigBSONHandler)
}
