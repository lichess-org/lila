package lila.storm

import com.github.blemale.scaffeine.LoadingCache
import com.roundeights.hasher.Algo
import scala.concurrent.duration._

import lila.common.config
import lila.common.config.Secret
import lila.common.ThreadLocalRandom
import lila.common.Uptime
import lila.memo.CacheApi
import lila.user.User

final class StormSign(secret: Secret, cacheApi: CacheApi) {

  private val store: LoadingCache[User.ID, String] =
    cacheApi.scaffeine
      .expireAfterAccess(24 hours)
      .build(_ => ThreadLocalRandom nextString 12)

  private val signer = Algo hmac secret.value

  def getPrev(user: User): String = store get user.id

  def check(user: User, signed: String): Boolean = {
    val correct =
      !Uptime.startedSinceMinutes(5) || {
        signer.sha1(s"${store.get(user.id)}:${user.id}") hash_= signed
      }
    if (correct) store.put(user.id, signed)
    correct
  }
}
