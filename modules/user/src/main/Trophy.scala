package lila.user

case class Trophy(
    _id: String, // random
    user: UserId,
    kind: TrophyKind,
    date: Instant,
    url: Option[String]
) extends Ordered[Trophy]:

  def timestamp = date.toMillis

  def compare(other: Trophy) =
    if kind.order == other.kind.order then date.compareTo(other.date)
    else Integer.compare(kind.order, other.kind.order)

  def anyUrl = url.orElse(kind.url)

case class TrophyKind(
    _id: String,
    name: String,
    icon: Option[String],
    url: Option[String],
    klass: Option[String],
    order: Int,
    withCustomImage: Boolean
)

object TrophyKind:
  export lila.core.user.TrophyKind.*
  val moderator = "moderator"
  val developer = "developer"
  val verified = "verified"
  val contentTeam = "contentTeam"
  val broadcastTeam = "broadcastTeam"
  val zugMiracle = "zugMiracle"

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
