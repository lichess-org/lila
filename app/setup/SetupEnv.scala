package lila
package setup

import com.mongodb.casbah.MongoCollection

final class SetupEnv(
  settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val configRepo = new UserConfigRepo(mongodb(MongoCollectionConfig))

  lazy val formFactory = new FormFactory(configRepo)
}
