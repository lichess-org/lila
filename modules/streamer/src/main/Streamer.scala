package lila.streamer

import lila.memo.PicfitImage
import lila.user.User

case class Streamer(
    _id: Streamer.Id,
    listed: Streamer.Listed,
    approval: Streamer.Approval,
    picture: Option[PicfitImage.Id],
    name: Streamer.Name,
    headline: Option[Streamer.Headline],
    description: Option[Streamer.Description],
    twitch: Option[Streamer.Twitch],
    youTube: Option[Streamer.YouTube],
    seenAt: Instant,         // last seen online
    liveAt: Option[Instant], // last seen streaming
    createdAt: Instant,
    updatedAt: Instant,
    lastStreamLang: Option[String] // valid 2 char language code or None
):

  inline def id = _id

  def userId = id.userId

  def hasPicture = picture.isDefined

  def isListed = listed.value && approval.granted

  def completeEnough = {
    twitch.isDefined || youTube.isDefined
  } && headline.isDefined && hasPicture

object Streamer:

  given UserIdOf[Streamer] = _.id.userId

  val imageSize = 350

  def make(user: User) =
    Streamer(
      _id = user.id into Id,
      listed = Listed(true),
      approval = Approval(
        requested = false,
        granted = false,
        ignored = user.marks.troll,
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
      seenAt = nowInstant,
      liveAt = none,
      createdAt = nowInstant,
      updatedAt = nowInstant,
      lastStreamLang = none
    )

  opaque type Id = String
  object Id extends OpaqueUserId[Id]

  opaque type Listed = Boolean
  object Listed extends YesNo[Listed]

  case class Approval(
      requested: Boolean,   // user requests a mod to approve
      granted: Boolean,     // a mod approved
      ignored: Boolean,     // further requests are ignored
      tier: Int,            // homepage featuring tier
      chatEnabled: Boolean, // embed chat inside lichess
      lastGrantedAt: Option[Instant]
  )
  opaque type Name = String
  object Name extends OpaqueString[Name]
  opaque type Headline = String
  object Headline extends OpaqueString[Headline]
  opaque type Description = String
  object Description extends OpaqueString[Description]

  case class Twitch(userId: String):
    def fullUrl = s"https://www.twitch.tv/$userId"
    def minUrl  = s"twitch.tv/$userId"
  object Twitch:
    private val UserIdRegex = """([a-zA-Z0-9](?:\w{2,24}+))""".r
    private val UrlRegex    = ("""twitch\.tv/""" + UserIdRegex + "").r.unanchored
    // https://www.twitch.tv/chessnetwork
    def parseUserId(str: String): Option[String] =
      str match
        case UserIdRegex(u) => u.some
        case UrlRegex(u)    => u.some
        case _              => none

  case class YouTube(channelId: String, liveVideoId: Option[String], pubsubVideoId: Option[String]):
    def fullUrl = s"https://www.youtube.com/channel/$channelId/live"
    def minUrl  = s"youtube.com/channel/$channelId/live"
  object YouTube:
    private val ChannelIdRegex = """^([\w-]{24})$""".r
    def parseChannelId(str: String): Option[String] =
      str match
        case ChannelIdRegex(c) => c.some
        case _                 => none

  trait WithContext:
    def streamer: Streamer
    def user: User
    def subscribed: Boolean
    def titleName = s"${user.title.fold("")(t => s"$t ")}${streamer.name}"

  case class WithUser(streamer: Streamer, user: User, subscribed: Boolean = false) extends WithContext
  case class WithUserAndStream(
      streamer: Streamer,
      user: User,
      stream: Option[Stream],
      subscribed: Boolean = false
  ) extends WithContext:
    def redirectToLiveUrl: Option[String] =
      stream so { s =>
        streamer.twitch.ifTrue(s.twitch).map(_.fullUrl) orElse
          streamer.youTube.ifTrue(s.youTube).map(_.fullUrl)
      }

  case class ModChange(list: Option[Boolean], tier: Option[Int], decline: Boolean)

  val maxTier = 10

  val tierChoices = (0 to maxTier).map(t => t -> t.toString)

  def canApply(u: User) = (u.count.game >= 15 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified
