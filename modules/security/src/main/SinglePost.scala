package lila
package security

import play.api.mvc.RequestHeader
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

  def consumeToken(encoded: String)(using RequestHeader): Boolean =
    encoded.split('|') match
      case Array(rnd, sign) if tokens.get(rnd) =>
        tokens.remove(rnd)
        if digestOf(rnd).hash_=(sign) then true
        else false
      case _ => false

  private def digestOf(rnd: String)(using req: RequestHeader) =
    signer.sha1(s"$rnd|${HTTPRequest.userAgent(req)}")

  def formMapping(using RequestHeader) =
    import play.api.data.Forms.*
    nonEmptyText.verifying("Session has expired, please try again", consumeToken)
