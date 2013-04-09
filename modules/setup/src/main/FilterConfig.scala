package lila.setup

import chess.{ Variant, Mode, Speed }
import lila.common.PimpedJson._

import play.api.libs.json._

case class FilterConfig(
    variant: Option[Variant],
    mode: Option[Mode],
    speed: Option[Speed],
    eloDiff: Int) {

  def withModeCasual = copy(mode = Mode.Casual.some)

  def encode = RawFilterConfig(
    v = ~variant.map(_.id),
    m = mode.map(_.id) | -1,
    s = ~speed.map(_.id),
    e = eloDiff
  )

  def >> = (
    variant map (_.id),
    mode map (_.id),
    speed map (_.id),
    eloDiff.some
  ).some

  def render = Json.obj(
    "variant" -> variant.map(_.toString),
    "mode" -> mode.map(_.toString),
    "speed" -> speed.map(_.id),
    "eloDiff" -> eloDiff
  )
}

object FilterConfig {

  val default = FilterConfig(
    variant = none,
    mode = none,
    speed = none,
    eloDiff = 0)

  val variants = 0 :: Config.variants
  val modes = -1 :: Mode.all.map(_.id)
  val speeds = 0 :: Config.speeds
  val eloDiffs = 0 :: 100 :: 200 :: 300 :: 500 :: Nil

  def <<(v: Option[Int], m: Option[Int], s: Option[Int], e: Option[Int]) = new FilterConfig(
    variant = v flatMap Variant.apply,
    mode = m flatMap Mode.apply,
    speed = s flatMap Speed.apply,
    eloDiff = ~e
  )

  def fromDB(obj: JsObject): Option[FilterConfig] = for {
    filter ← obj obj "filter"
    variant ← filter int "v"
    mode ← filter int "m"
    speed ← filter int "s"
    eloDiff ← filter int "e"
    config ← RawFilterConfig(variant, mode, speed, eloDiff).decode
  } yield config

  import lila.db.Tube
  import play.api.libs.json._

  private[setup] lazy val tube = Tube(
    reader = Reads[FilterConfig](js ⇒
      ~(for {
        obj ← js.asOpt[JsObject]
        raw ← RawFilterConfig.tube.read(obj).asOpt
        decoded ← raw.decode
      } yield JsSuccess(decoded): JsResult[FilterConfig])
    ),
    writer = Writes[FilterConfig](config ⇒
      RawFilterConfig.tube.write(config.encode) getOrElse JsUndefined("[setup] Can't write config")
    )
  )
}

private[setup] case class RawFilterConfig(v: Int, m: Int, s: Int, e: Int) {

  def decode = FilterConfig(
    variant = Variant(v),
    mode = Mode(m),
    speed = Speed(s),
    eloDiff = e
  ).some
}

private[setup] object RawFilterConfig {

  import lila.db.Tube
  import play.api.libs.json.Json

  private[setup] lazy val tube = Tube(
    reader = Json.reads[RawFilterConfig],
    writer = Json.writes[RawFilterConfig])
}
