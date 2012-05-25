package lila
package core

import play.api._
import play.api.cache._

class MongoCachePlugin(app: Application) extends CachePlugin {

  lazy val mongoCache = Global.env.mongoCache

  lazy val api = new CacheAPI {

    def set(key: String, value: Any, expiration: Int) {
      mongoCache.set(key, value)
    }

    def get(key: String): Option[Any] = mongoCache get key

    def remove(key: String) {
      mongoCache remove key
    }
  }
}
