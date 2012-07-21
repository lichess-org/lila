package lila
package modlog

import com.mongodb.casbah.MongoCollection

import core.Settings

final class ModlogEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val modlogRepo = new ModlogRepo(mongodb(ModlogCollectionModlog))

  lazy val api = new ModlogApi(
    repo = modlogRepo)
}
