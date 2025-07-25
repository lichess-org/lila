package lila.db

/* Will try to compute the future only once every `expireAfterMillis`,
 * and serve the current future to further `get` calls.
 * Not thread safe: it can compute the future more than once every `expireAfterMillis`.
 * It's a deliberate design choice to improve performance
 * in the cases where accidental duplicate calls don't matter. */
final private class SingleFutureCache[A](compute: () => Fu[A], expireAfterMillis: Int):

  private var current: Fu[A] = fufail("SingleFutureCache.empty")
  private var expiresAt: Long = 0

  def get: Fu[A] =
    val now = nowMillis
    if now > expiresAt then
      expiresAt = now + expireAfterMillis
      current = compute()
    current
