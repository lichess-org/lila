package lila
package security

import play.api.mvc.RequestHeader
import play.api.data.Forms.*
import scalalib.SecureRandom

import lila.core.config.Secret
import lila.core.security.{ SinglePostToken, SinglePostMakeToken }
import lila.common.HTTPRequest
import play.api.data.Mapping

final class SinglePost(secret: Secret, settingStore: lila.memo.SettingStore.Builder)(using Executor):

  private val signer = com.roundeights.hasher.Algo.hmac(secret.value)

  private val tokens = scalalib.cache.ExpireSetMemo[String](10.minutes)

  val enforce = settingStore[Boolean](
    "singlePostEnforce",
    default = true,
    text = "Enforce single post".some
  )

  val newToken: SinglePostMakeToken = req ?=>
    val rnd = SecureRandom.nextString(16)
    tokens.put(rnd)
    lila.mon.security.singlePost.newToken(HTTPRequest.actionName(req)).increment()
    SinglePostToken(s"$rnd|${digestOf(rnd).hex}")

  def consumeToken(token: String)(using RequestHeader): Boolean =
    token.split('|') match
      case Array(rnd, sign) =>
        if !tokens.get(rnd) then result("expired".some)
        else if !digestOf(rnd).hash_=(sign) then result("badSign".some)
        else
          tokens.remove(rnd)
          result(none)
      case _ => result("missing".some)

  private def result(err: Option[String])(using req: RequestHeader) =
    val cold = !lila.common.Uptime.startedSinceMinutes(5)
    val endpoint = HTTPRequest.actionName(req)
    lila.mon.security.singlePost.consume(endpoint, err | "success").increment()
    err
      .filterNot(_ == "expired" && cold)
      .foreach: e =>
        logger
          .branch("singlePost")
          .warn(s"$endpoint $e ${HTTPRequest.printReq(req)} ${HTTPRequest.printClient(req)}")
    err.isEmpty || !enforce.get() || cold

  private def digestOf(rnd: String)(using req: RequestHeader) =
    signer.sha1(s"$rnd|${HTTPRequest.userAgent(req)}")

  def formMapping(using RequestHeader) =
    nonEmptyText.verifying("Session has expired, please try again", consumeToken)

  def formPair(using RequestHeader): (String, Mapping[String]) = "singlePost" -> formMapping

  def formPairWithLichobileCompat(using req: RequestHeader): (String, Mapping[String]) =
    if HTTPRequest.isLichobile(req)
    then "singlePost" -> optional(text).transform(~_, _.some)
    else formPair

  def presenceForm = play.api.data.Form(single("singlePost" -> nonEmptyText))
