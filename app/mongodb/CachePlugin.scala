package lila
package mongodb

import lila.core.Global

import play.api.Application
import play.api.cache.{ CacheAPI, CachePlugin => PlayCachePlugin }

class CachePlugin(app: Application) extends PlayCachePlugin {

  lazy val cache = Global.env.mongodb.cache

  lazy val api = new CacheAPI {

    def set(key: String, value: Any, expiration: Int) {
      cache.set(key, value)
    }

    def get(key: String): Option[Any] = cache get key

    def remove(key: String) {
      cache remove key
    }
  }
}
