package lila.relay

import play.api.mvc.RequestHeader
import lila.core.security.LilaCookie
import play.api.mvc.Result

enum RelayVideoEmbed:
  case No
  case Auto
  case Stream(userId: UserId)
  override def toString = this match
    case No        => "no"
    case Auto      => ""
    case Stream(u) => u.toString

final class RelayVideoEmbedStore(baker: LilaCookie):

  import RelayVideoEmbed.*
  private val cookieName = "relayVideo"

  def read(using req: RequestHeader): RelayVideoEmbed =
    def fromCookie = req.cookies.get(cookieName).map(_.value).filter(_.nonEmpty) match
      case Some("no") => No
      case _          => Auto
    req.queryString.get("embed") match
      case Some(Nil)       => fromCookie
      case Some(Seq(""))   => Auto
      case Some(Seq("no")) => No
      case Some(Seq(name)) => UserStr.read(name).fold(Auto)(u => Stream(u.id))
      case _               => fromCookie

  def write(embed: RelayVideoEmbed)(using RequestHeader) = baker.cookie(
    name = cookieName,
    value = embed.toString,
    maxAge = some(60 * 60 * 3), // 3h
    httpOnly = false.some
  )

  def withCookie(f: RelayVideoEmbed => Fu[Result])(using RequestHeader, Executor): Fu[Result] =
    val embed = read
    f(embed).map:
      _.withCookies(write(embed))
