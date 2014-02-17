package lila.setup

import chess.{ Variant, Mode, Speed }
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.rating.RatingRange

case class FilterConfig(
    variant: List[Variant],
    mode: List[Mode],
    speed: List[Speed],
    ratingRange: RatingRange) {

  def encode = RawFilterConfig(
    v = variant.map(_.id),
    m = mode.map(_.id),
    s = speed.map(_.id),
    e = ratingRange.toString)

  def >> = (
    variant map (_.id),
    mode map (_.id),
    speed map (_.id),
    ratingRange.toString
  ).some

  def render = Json.obj(
    "variant" -> variant.map(_.toString),
    "mode" -> mode.map(_.toString),
    "speed" -> speed.map(_.id),
    "rating" -> ratingRange.notBroad.map(rr => List(rr.min, rr.max)))

  def nonEmpty = copy(
    variant = variant.isEmpty.fold(FilterConfig.default.variant, variant),
    mode = mode.isEmpty.fold(FilterConfig.default.mode, mode),
    speed = speed.isEmpty.fold(FilterConfig.default.speed, speed))
}

object FilterConfig {

  val variants = List(Variant.Standard, Variant.Chess960)
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

  def fromDB(obj: JsObject): Option[FilterConfig] = for {
    filter ← obj obj "filter"
    variant ← filter ints "v"
    mode ← filter ints "m"
    speed ← filter ints "s"
    ratingRange ← filter str "e"
    config ← RawFilterConfig(variant, mode, speed, ratingRange).decode
  } yield config

  import lila.db.JsTube
  import play.api.libs.json._

  private[setup] lazy val tube = JsTube(
    reader = Reads[FilterConfig](js =>
      ~(for {
        obj ← js.asOpt[JsObject]
        raw ← RawFilterConfig.tube.read(obj).asOpt
        decoded ← raw.decode
      } yield JsSuccess(decoded): JsResult[FilterConfig])
    ),
    writer = Writes[FilterConfig](config =>
      RawFilterConfig.tube.write(config.encode) getOrElse JsUndefined("[setup] Can't write config")
    )
  )
}

private[setup] case class RawFilterConfig(v: List[Int], m: List[Int], s: List[Int], e: String) {

  def decode = FilterConfig(
    variant = v map Variant.apply flatten,
    mode = m map Mode.apply flatten,
    speed = s map Speed.apply flatten,
    ratingRange = RatingRange orDefault e
  ).nonEmpty.some
}

private[setup] object RawFilterConfig {

  import lila.db.JsTube
  import play.api.libs.json.Json

  private[setup] lazy val tube = JsTube(
    reader = Json.reads[RawFilterConfig],
    writer = Json.writes[RawFilterConfig])
}
