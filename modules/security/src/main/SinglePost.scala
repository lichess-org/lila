package lila
package security

import play.api.mvc.{ RequestHeader, Request }
import play.api.data.Forms.*
import scalalib.SecureRandom

import lila.core.config.Secret
import lila.core.security.{ SinglePostToken, SinglePostMakeToken }
import lila.common.HTTPRequest

final class SinglePost(secret: Secret)(using Executor):

  private val signer = com.roundeights.hasher.Algo.hmac(secret.value)

  private val tokens = scalalib.cache.ExpireSetMemo[String](10.minutes)

  val newToken: SinglePostMakeToken = _ ?=>
    val rnd = SecureRandom.nextString(12)
    tokens.put(rnd)
    SinglePostToken(s"$rnd|${digestOf(rnd).hex}")

  def dryTest(token: String)(using RequestHeader): Option[String] =
    token.split('|') match
      case Array(rnd, sign) if tokens.get(rnd) && digestOf(rnd).hash_=(sign) => rnd.some
      case _ => none

  def consumeToken(token: String)(using RequestHeader): Boolean =
    val rnd = dryTest(token)
    rnd.foreach(tokens.remove)
    rnd.isDefined

  private def digestOf(rnd: String)(using req: RequestHeader) =
    signer.sha1(s"$rnd|${HTTPRequest.userAgent(req)}")

  def formMapping(using RequestHeader) =
    nonEmptyText.verifying("Session has expired, please try again", consumeToken)

  def formPair(using RequestHeader) = "singlePost" -> formMapping

  def presenceForm = play.api.data.Form(single("singlePost" -> nonEmptyText))
