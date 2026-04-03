package lila
package security

import scalalib.SecureRandom

import lila.core.config.Secret

final class SinglePost(secret: Secret)(using Executor):

  private val signer = com.roundeights.hasher.Algo.hmac(secret.value)

  private val tokens = scalalib.cache.ExpireSetMemo[String](10.minutes)

  def newToken: String =
    val rnd = SecureRandom.nextString(12)
    tokens.put(rnd)
    val signed = signer.sha1(rnd).hex
    s"$rnd|$signed".pp("new")

  def consumeToken(encoded: String): Boolean = {
    encoded.pp("consumable").split('|') match
      case Array(rnd, sign) if tokens.get(rnd).pp("found") =>
        tokens.remove(rnd)
        if signer.sha1(rnd) hash_= sign then true
        else false
      case _ => false
  }.pp("consumed")

  val formMapping =
    import play.api.data.Forms.*
    nonEmptyText.verifying("Invalid CSRF token, please try again", consumeToken)
