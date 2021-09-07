package lila.streamer

import org.joda.time.DateTime

import lila.memo.{ PicfitImage, PicfitUrl }
import lila.user.User

case class Streamer(
    _id: Streamer.Id, // user ID
    listed: Streamer.Listed,
    approval: Streamer.Approval,
    picture: Option[PicfitImage.Id],
    name: Streamer.Name,
    headline: Option[Streamer.Headline],
    description: Option[Streamer.Description],
    twitch: Option[Streamer.Twitch],
    youTube: Option[Streamer.YouTube],
    seenAt: DateTime,         // last seen online
    liveAt: Option[DateTime], // last seen streaming
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def userId = _id.value

  def is(user: User) = userId == user.id

  def hasPicture = picture.isDefined

  def isListed = listed.value && approval.granted

  def completeEnough = {
    twitch.isDefined || youTube.isDefined
  } && headline.isDefined && hasPicture
}

object Streamer {

  def make(user: User) =
    Streamer(
      _id = Id(user.id),
      listed = Listed(true),
      approval = Approval(
        requested = false,
        granted = false,
        ignored = false,
        tier = 0,
        chatEnabled = true,
        lastGrantedAt = none
      ),
      picture = none,
      name = Name(user.realNameOrUsername),
      headline = none,
      description = none,
      twitch = none,
      youTube = none,
      seenAt = DateTime.now,
      liveAt = none,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

  case class Id(value: User.ID)     extends AnyVal with StringValue
  case class Listed(value: Boolean) extends AnyVal
  case class Approval(
      requested: Boolean,   // user requests a mod to approve
      granted: Boolean,     // a mod approved
      ignored: Boolean,     // further requests are ignored
      tier: Int,            // homepage featuring tier
      chatEnabled: Boolean, // embed chat inside lichess
      lastGrantedAt: Option[DateTime]
  )
  case class PicturePath(value: String) extends AnyVal with StringValue
  case class Name(value: String)        extends AnyVal with StringValue
  case class Headline(value: String)    extends AnyVal with StringValue
  case class Description(value: String) extends AnyVal with StringValue

  case class Twitch(userId: String) {
    def fullUrl = s"https://www.twitch.tv/$userId"
    def minUrl  = s"twitch.tv/$userId"
  }
  object Twitch {
    private val UserIdRegex = """([a-zA-Z0-9](?:\w{2,24}+))""".r
    private val UrlRegex    = ("""twitch\.tv/""" + UserIdRegex + "").r.unanchored
    // https://www.twitch.tv/chessnetwork
    def parseUserId(str: String): Option[String] =
      str match {
        case UserIdRegex(u) => u.some
        case UrlRegex(u)    => u.some
        case _              => none
      }
  }

  case class YouTube(channelId: String) {
    def fullUrl = s"https://www.youtube.com/channel/$channelId"
    def minUrl  = s"youtube.com/channel/$channelId"
  }
  object YouTube {
    private val ChannelIdRegex = """^([\w-]{24})$""".r
    private val UrlRegex       = """youtube\.com/channel/([\w-]{24})""".r.unanchored
    def parseChannelId(str: String): Option[String] =
      str match {
        case ChannelIdRegex(c) => c.some
        case UrlRegex(c)       => c.some
        case _                 => none
      }
  }

  case class WithUser(streamer: Streamer, user: User) {
    def titleName = s"${user.title.fold("")(t => s"$t ")}${streamer.name}"
  }
  case class WithUserAndStream(streamer: Streamer, user: User, stream: Option[Stream]) {
    def withoutStream = WithUser(streamer, user)
    def titleName     = withoutStream.titleName

    def redirectToLiveUrl: Option[String] =
      stream ?? { s =>
        streamer.twitch.ifTrue(s.twitch).map(_.fullUrl) orElse
          streamer.youTube.ifTrue(s.youTube).map(_.fullUrl)
      }
  }

  case class ModChange(list: Option[Boolean], tier: Option[Int], decline: Boolean)

  val maxTier = 10

  val tierChoices = (0 to maxTier).map(t => t -> t.toString)

  def canApply(u: User) = (u.count.game >= 15 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified
}
