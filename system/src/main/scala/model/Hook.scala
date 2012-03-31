package lila.system
package model

import com.novus.salat.annotations.Key
import com.mongodb.DBRef

case class Hook(
    @Key("_id") id: String,
    ownerId: String,
    variant: Int,
    time: Option[Int],
    increment: Option[Int],
    mode: Int,
    color: String,
    username: String,
    elo: Option[Int],
    `match`: Boolean,
    eloRange: Option[String],
    engine: Boolean,
    game: Option[DBRef]) {

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
    "clock" -> ((time |@| increment apply renderClock _) | "Unlimited"),
    "emin" -> eloMin,
    "emax" -> eloMax
  ) +? (engine, "engine" -> true)

  def renderClock(time: Int, inc: Int) = "%d + %d".format(time, inc)
}
