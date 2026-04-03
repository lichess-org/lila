package lila
package security

import scalalib.SecureRandom

import lila.core.config.Secret
import lila.core.security.{ SinglePostToken, SinglePostMakeToken }

final class SinglePost(secret: Secret)(using Executor):

  private val signer = com.roundeights.hasher.Algo.hmac(secret.value)

  private val tokens = scalalib.cache.ExpireSetMemo[String](10.minutes)

  val newToken: SinglePostMakeToken = () =>
    val rnd = SecureRandom.nextString(12)
    tokens.put(rnd)
    val signed = signer.sha1(rnd).hex
    SinglePostToken(s"$rnd|$signed")

  def consumeToken(encoded: String): Boolean =
    encoded.split('|') match
      case Array(rnd, sign) if tokens.get(rnd) =>
        tokens.remove(rnd)
        if signer.sha1(rnd) hash_= sign then true
        else false
      case _ => false

  val formMapping =
    import play.api.data.Forms.*
    nonEmptyText.verifying("Session has expired, please try again", consumeToken)
