package lila.web

import akka.stream.scaladsl.Source
import play.api.http.*
import play.api.libs.json.*
import play.api.mvc.*

trait ResponseBuilder(using Executor)
    extends ControllerHelpers
    with ResponseWriter
    with ResponseHeaders
    with CtrlExtensions
    with CtrlErrors:

  export scalatags.Text.Frag

  given Conversion[Result, Fu[Result]] = fuccess(_)

  val rateLimitedMsg = "Too many requests. Try again later."
  val rateLimitedJson = TooManyRequests(jsonError(rateLimitedMsg))

  val jsonOkBody = Json.obj("ok" -> true)
  val jsonOkResult = JsonOk(jsonOkBody)
  def jsonOkMsg(msg: String) = JsonOk(Json.obj("ok" -> msg))

  def JsonOk(body: JsValue): Result = Ok(body).as(JSON)
  def JsonOk[A: Writes](body: A): Result = Ok(Json.toJson(body)).as(JSON)
  def JsonOk[A: Writes](fua: Fu[A]): Fu[Result] = fua.dmap(JsonOk)
  def JsonOptionOk[A: Writes](fua: Fu[Option[A]]) = fua.map(_.fold(notFoundJson())(JsonOk))
  def JsonStrOk(str: JsonStr): Result = Ok(str).as(JSON)
  def JsonBadRequest(body: JsValue): Result = BadRequest(body).as(JSON)
  def JsonBadRequest(msg: String): Result = JsonBadRequest(jsonError(msg))

  def strToNdJson(source: Source[String, ?]): Result =
    Ok.chunked(source).as(ndJson.contentType).noProxyBuffer

  def jsToNdJson(source: Source[JsValue, ?]): Result =
    strToNdJson(ndJson.jsToString(source))

  def jsOptToNdJson(source: Source[Option[JsValue], ?]): Result =
    strToNdJson(ndJson.jsOptToString(source))

  /* We roll our own action, as we don't want to compose play Actions. */
  def action[A](parser: BodyParser[A])(handler: Request[A] ?=> Fu[Result]): EssentialAction = new:
    import play.api.libs.streams.Accumulator
    import akka.util.ByteString
    def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
      parser(rh).mapFuture:
        case Left(r) => fuccess(r)
        case Right(a) => handler(using Request(rh, a))

  def redirectWithQueryString(path: String)(using req: RequestHeader) =
    Redirect:
      if req.target.uriString.contains("?")
      then s"$path?${req.target.queryString}"
      else path

  private val movedMap: Map[String, String] = Map(
    "yt" -> "https://youtube.com/@LichessDotOrg",
    "dmca" -> "https://docs.google.com/forms/d/e/1FAIpQLSdRVaJ6Wk2KHcrLcY0BxM7lTwYSQHDsY2DsGwbYoLUBo3ngfQ/viewform",
    "fishnet" -> "https://github.com/lichess-org/fishnet",
    "qa" -> "/faq",
    "help" -> "/contact",
    "support" -> "/contact",
    "donate" -> "/patron",
    "how-to-cheat" -> "/page/how-to-cheat"
  )
  def staticRedirect(key: String): Option[Fu[Result]] = movedMap.get(key).map { MovedPermanently(_) }
