package lila.system
package model

case class Hook(
    id: String,
    ownerId: String,
    variant: Int,
    hasClock: Boolean,
    time: Option[Int],
    increment: Option[Int],
    mode: Int,
    color: String,
    username: String,
    elo: Option[Int],
    `match`: Boolean,
    eloRange: Option[String],
    engine: Boolean) {

  def realVariant = Variant(variant) | Standard

  def realMode = Mode(mode) | Casual

  def eloMin = eloRange map (_ takeWhile ('-' !=))

  def eloMax = eloRange map (_ dropWhile ('-' !=) tail)

  def render = Map(
    "username" -> username,
    "elo" -> elo,
    "variant" -> realVariant.toString,
    "mode" -> realMode.toString,
    "color" -> color,
    "clock" -> (if (hasClock) time + " + " + increment else "Unlimited"),
    "emin" -> eloMin,
    "emax" -> eloMax
  ) +? (engine, "engine" -> true)
}
