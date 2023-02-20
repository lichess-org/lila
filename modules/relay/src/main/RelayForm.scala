package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import io.mola.galimatias.URL
import scala.util.Try

import lila.security.Granter
import lila.user.User
import lila.common.Form.{ cleanNonEmptyText, cleanText }

final class RelayForm {

  import RelayForm._
  import lila.common.Form.ISODateTimeOrTimestamp

  val form = Form(
    mapping(
      "name"        -> cleanText(minLength = 3, maxLength = 80),
      "description" -> cleanText(minLength = 3, maxLength = 400),
      "markup"      -> optional(cleanText(maxLength = 20000)),
      "official"    -> optional(boolean),
      "syncUrl" -> optional {
        nonEmptyText.verifying("Invalid source", validSource _)
      },
      "credit"   -> optional(cleanNonEmptyText),
      "startsAt" -> optional(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp),
      "throttle" -> optional(number(min = 2, max = 60))
    )(Data.apply)(Data.unapply)
  )

  def create = form

  def edit(r: Relay) = form fill Data.make(r)
}

object RelayForm {

  private def validSource(source: String) =
    cleanUrl(source).isDefined

  private def cleanUrl(source: String): Option[String] =
    for {
      url <- Try(URL.parse(source)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toString)
      // prevent common mistakes (not for security)
      if !blocklist.exists(subdomain(host, _))
    } yield url.toString.stripSuffix("/")

  private def subdomain(host: String, domain: String) = s".$host".endsWith(s".$domain")

  private val blocklist = List(
    "localhost",
    "127.0.0.1",
    "::1",
    "twitch.tv",
    "twitch.com",
    "youtube.com",
    "youtu.be",
    "lishogi.org",
    "lichess.org",
    "google.com",
    "chess.com",
    "vk.com",
    "192.168.1.154",
    "chess-results.com",
    "chessgames.com"
  )

  case class Data(
      name: String,
      description: String,
      markup: Option[String],
      official: Option[Boolean],
      syncUrl: Option[String],
      credit: Option[String],
      startsAt: Option[DateTime],
      throttle: Option[Int]
  ) {

    def update(relay: Relay, user: User) =
      relay.copy(
        name = name,
        description = description,
        markup = markup,
        official = ~official && Granter(_.Relay)(user),
        sync = makeSync(user),
        credit = credit,
        startsAt = startsAt,
        finished = relay.finished && startsAt.fold(true)(_.isBeforeNow)
      )

    def makeSync(user: User) =
      Relay.Sync(
        upstream = syncUrl.flatMap(cleanUrl) map { u =>
          Relay.Sync.Upstream(u)
        },
        until = none,
        nextAt = none,
        delay = throttle ifTrue Granter(_.Relay)(user),
        log = SyncLog.empty
      )

    def make(user: User) =
      Relay(
        _id = Relay.makeId,
        name = name,
        description = description,
        markup = markup,
        ownerId = user.id,
        sync = makeSync(user),
        credit = credit,
        likes = lila.study.Study.Likes(1),
        createdAt = DateTime.now,
        finished = false,
        official = ~official && Granter(_.Relay)(user),
        startsAt = startsAt,
        startedAt = none
      )
  }

  object Data {

    def make(relay: Relay) =
      Data(
        name = relay.name,
        description = relay.description,
        markup = relay.markup,
        official = relay.official option true,
        syncUrl = relay.sync.upstream.map(_.url),
        credit = relay.credit,
        startsAt = relay.startsAt,
        throttle = relay.sync.delay
      )
  }
}
