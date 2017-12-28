package lila.streamer

import org.joda.time.DateTime

import lila.user.User

case class Streamer(
    _id: Streamer.Id, // user ID
    listed: Streamer.Listed,
    autoFeatured: Streamer.AutoFeatured,
    picturePath: Option[Streamer.PicturePath],
    description: Option[Streamer.Description],
    twitch: Option[Streamer.Twitch],
    youTube: Option[Streamer.YouTube],
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def is(user: User) = id.value == user.id

  def hasPicture = picturePath.isDefined
}

object Streamer {

  def make(user: User) = Streamer(
    _id = Id(user.id),
    listed = Listed(true),
    autoFeatured = AutoFeatured(false),
    picturePath = none,
    description = none,
    twitch = none,
    youTube = none,
    createdAt = DateTime.now,
    updatedAt = DateTime.now
  )

  case class Id(value: User.ID) extends AnyVal with StringValue
  case class Listed(value: Boolean) extends AnyVal
  case class AutoFeatured(value: Boolean) extends AnyVal
  case class PicturePath(value: String) extends AnyVal with StringValue
  case class Description(value: String) extends AnyVal with StringValue
  case class Twitch(userId: String, live: Live)
  case class YouTube(channelId: String, live: Live)
  case class Live(liveAt: Option[DateTime], checkedAt: Option[DateTime]) {
    def now = liveAt ?? { l =>
      checkedAt ?? { l == }
    }
  }
}
