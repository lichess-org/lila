package lila.db

import reactivemongo.api._

final class Mongo2Env(val db: DB, settings: Settings) {

  import settings._

  def apply(name: String): ReactiveColl = db(name)
}
