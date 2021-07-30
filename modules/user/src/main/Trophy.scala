package lila.user

import org.joda.time.DateTime

case class Trophy(
    _id: String, // random
    user: String,
    kind: TrophyKind,
    date: DateTime,
    url: Option[String]
) extends Ordered[Trophy] {

  def timestamp = date.getMillis

  def compare(other: Trophy) =
    if (kind.order == other.kind.order) date compareTo other.date
    else Integer.compare(kind.order, other.kind.order)
}

case class TrophyKind(
    _id: String,
    name: String,
    icon: Option[String],
    url: Option[String],
    klass: Option[String],
    order: Int,
    withCustomImage: Boolean
)

object TrophyKind {
  val marathonWinner         = "marathonWinner"
  val marathonTopTen         = "marathonTopTen"
  val marathonTopFifty       = "marathonTopFifty"
  val marathonTopHundred     = "marathonTopHundred"
  val marathonTopFivehundred = "marathonTopFivehundred"
  val moderator              = "moderator"
  val developer              = "developer"
  val verified               = "verified"
  val zugMiracle             = "zugMiracle"

  object Unknown
      extends TrophyKind(
        _id = "unknown",
        name = "Unknown",
        order = 0,
        url = none,
        icon = none,
        klass = none,
        withCustomImage = false
      )
}
