package lila.app
package game

import play.api.Application
import com.mongodb.casbah.MongoCollection

import core.Settings

final class GameEnv(
    app: Application,
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._
}
