package lila.db

import reactivemongo.api._

final class Mongo2Env(val db: DB, settings: Settings) {

  import settings._
}
