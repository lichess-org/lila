package lila
package setup

import chess.{ Variant, Mode, Speed }

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
    eloDiff
  ).some

  def render = Map(
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

  def <<(v: Option[Int], m: Option[Int], s: Option[Int], e: Int) = new FilterConfig(
    variant = v flatMap Variant.apply,
    mode = m flatMap Mode.apply,
    speed = s flatMap Speed.apply,
    eloDiff = e
  )

  import com.mongodb.casbah.Imports._

  def fromDB(obj: DBObject): Option[FilterConfig] = for {
    filter ← obj.getAs[DBObject]("filter")
    variant ← filter.getAs[Int]("v")
    mode ← filter.getAs[Int]("m")
    speed ← filter.getAs[Int]("s")
    eloDiff ← filter.getAs[Int]("e")
    config ← RawFilterConfig(variant, mode, speed, eloDiff).decode
  } yield config
}

private[setup] case class RawFilterConfig(v: Int, m: Int, s: Int, e: Int) {

  def decode = FilterConfig(
    variant = Variant(v),
    mode = Mode(m),
    speed = Speed(s),
    eloDiff = e
  ).some
}
