package lila.streamer

import play.api.i18n.Lang
import play.api.libs.json.*
import cats.derived.*
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.model.Language

import lila.core.id.ImageId
import lila.common.Json.given
import lila.common.String.removeMultibyteSymbols
import lila.core.config.NetDomain
import lila.core.i18n.toLanguage
import lila.db.dsl.given

trait Stream:
  val streamer: Streamer
  def platform: Platform
  def status: Html
  def lang: Lang
  def urls: Stream.Urls

  def is[U: UserIdOf](u: U): Boolean = streamer.is(u)
  def twitch = platform == "twitch"
  def youtube = platform == "youtube"
  def language = toLanguage(lang)

  lazy val cleanStatus = status.map(s => removeMultibyteSymbols(s).trim)

object Stream:

  case class Keyword(value: String) extends AnyRef with StringValue:
    def toLowerCase = value.toLowerCase

  case class Urls(embed: NetDomain => String, redirect: String):
    def toPair(domain: NetDomain) = (embed(domain), redirect)

  def toLichessJson(picfit: lila.memo.PicfitUrl, stream: Stream) = Json.obj(
    "stream" -> Json.obj(
      "service" -> stream.platform,
      "status" -> stream.status,
      "lang" -> stream.lang
    ),
    "streamer" -> Json
      .obj("name" -> stream.streamer.name.value)
      .add("headline" -> stream.streamer.headline)
      .add("description" -> stream.streamer.description)
      .add("twitch" -> stream.streamer.twitch.map(_.fullUrl))
      .add("youtube" -> stream.streamer.youtube.map(_.fullUrl))
      .add("image" -> stream.streamer.picture.map: pic =>
        picfit.thumbnail(pic)(Streamer.imageDimensions))
  )

case class Streamer(
    @Key("_id") id: Streamer.Id,
    listed: Streamer.Listed,
    approval: Streamer.Approval,
    picture: Option[ImageId],
    name: Streamer.Name,
    headline: Option[Streamer.Headline],
    description: Option[Streamer.Description],
    twitch: Option[Streamer.Twitch],
    youtube: Option[Streamer.Youtube],
    seenAt: Instant, // last seen online
    liveAt: Option[Instant], // last seen streaming
    createdAt: Instant,
    updatedAt: Instant,
    lastStreamLang: Option[Language]
):
  def userId = id.userId

  def hasPicture = picture.isDefined

  def isListed = listed.value && approval.granted

  def completeEnough = {
    twitch.isDefined || youtube.isDefined
  } && name.value.length > 2 && hasPicture

object Streamer:

  opaque type Id = String
  object Id extends lila.core.userId.OpaqueUserId[Id]
  opaque type Listed = Boolean
  object Listed extends YesNo[Listed]
  opaque type Name = String
  object Name extends OpaqueString[Name]
  opaque type Headline = String
  object Headline extends OpaqueString[Headline]
  opaque type Description = String
  object Description extends OpaqueString[Description]

  given UserIdOf[Streamer] = _.id.userId

  val maxNameLength = 30
  val imageDimensions = lila.memo.Dimensions.square(350)

  def make(user: User) =
    Streamer(
      id = user.id.into(Id),
      listed = Listed(true),
      approval = Approval(
        requested = false,
        granted = false,
        ignored = user.marks.troll,
        tier = 0,
        chatEnabled = true,
        lastGrantedAt = none,
        reason = none
      ),
      picture = none,
      name = Name(user.realNameOrUsername.take(maxNameLength)),
      headline = none,
      description = none,
      twitch = none,
      youtube = none,
      seenAt = nowInstant,
      liveAt = none,
      createdAt = nowInstant,
      updatedAt = nowInstant,
      lastStreamLang = none
    )

  case class Approval(
      requested: Boolean, // user requests a mod to approve
      granted: Boolean, // a mod approved
      ignored: Boolean, // further requests are ignored
      tier: Int, // homepage featuring tier
      chatEnabled: Boolean, // embed chat inside lichess
      lastGrantedAt: Option[Instant],
      reason: Option[String]
  )

  import lila.streamer.Twitch.{ TwitchId, TwitchLogin }
  case class Twitch(id: TwitchId, login: TwitchLogin) derives Eq:
    def fullUrl = s"https://www.twitch.tv/$login"
    def minUrl = s"twitch.tv/$login"

  object Twitch:

    private val LoginRegex = """([a-zA-Z0-9](?:\w{2,24}+))""".r
    private val UrlRegex = ("""twitch\.tv/""" + LoginRegex + "").r.unanchored
    given Reads[Twitch] = Json.reads
    // https://www.twitch.tv/chessnetwork
    def parseLogin(str: String): Option[TwitchLogin] =
      str match
        case LoginRegex(u) => TwitchLogin(u).some
        case UrlRegex(u) => TwitchLogin(u).some
        case _ => none

  case class Youtube(channelId: String, liveVideoId: Option[String], pubsubVideoId: Option[String])
      derives Eq:
    def fullUrl = s"https://www.youtube.com/channel/$channelId/live"
    def minUrl = s"youtube.com/channel/$channelId/live"
  object Youtube:
    private val ChannelIdRegex = """^([\w-]{24})$""".r
    def parseChannelId(str: String): Option[String] =
      str match
        case ChannelIdRegex(c) => c.some
        case _ => none

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
      stream.so: s =>
        streamer.twitch
          .ifTrue(s.twitch)
          .map(_.fullUrl)
          .orElse(streamer.youtube.ifTrue(s.youtube).map(_.fullUrl))

  case class ModChange(list: Option[Boolean], tier: Option[Int], decline: Boolean, reason: Option[String])

  val maxTier = 10

  val tierChoices = (0 to maxTier).map(t => t -> t.toString)

  def canApply(u: User) = (u.count.game >= 15 && u.createdSinceDays(2)) || u.hasTitle || u.isVerified

private object BsonHandlers:

  given BSONDocumentHandler[Streamer.Youtube] = Macros.handler
  given BSONDocumentHandler[Streamer.Twitch] = Macros.handler
  given BSONDocumentHandler[Streamer.Approval] = Macros.handler
  given BSONDocumentHandler[Streamer] = Macros.handler
