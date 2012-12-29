package lila
package setup

import chess.{ Variant, Mode, Speed }

case class FilterConfig(
    variant: Option[Variant],
    mode: Option[Mode],
    speed: Option[Speed]) {

  def encode = RawFilterConfig(
    v = ~variant.map(_.id),
    m = mode.map(_.id) | -1,
    s = ~speed.map(_.id)
  )

  def >> = Some((
    variant map (_.id),
    mode map (_.id),
    speed map (_.id)
  ))

  def render = Map(
    "variant" -> variant.map(_.toString),
    "mode" -> mode.map(_.toString),
    "speed" -> speed.map(_.toString)
  )
}

object FilterConfig {

  val default = FilterConfig(
    variant = none,
    mode = none,
    speed = none)

  val variants = 0 :: Config.variants
  val modes = -1 :: Mode.all.map(_.id)
  val speeds = 0 :: Config.speeds

  def <<(v: Option[Int], m: Option[Int], s: Option[Int]) = new FilterConfig(
    variant = v flatMap Variant.apply,
    mode = m flatMap Mode.apply,
    speed = s flatMap Speed.apply
  )

  import com.mongodb.casbah.Imports._

  def fromDB(obj: DBObject): Option[FilterConfig] = for {
    filter ← obj.getAs[DBObject]("filter")
    variant ← filter.getAs[Int]("v")
    mode ← filter.getAs[Int]("m")
    speed ← filter.getAs[Int]("s")
    config ← RawFilterConfig(variant, mode, speed).decode
  } yield config
}

private[setup] case class RawFilterConfig(
    v: Int,
    m: Int,
    s: Int) {

  def decode = FilterConfig(
    variant = Variant(v),
    mode = Mode(m),
    speed = Speed(s)
  ).some
}
