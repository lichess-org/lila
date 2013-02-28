package lila.db

import reactivemongo.api._

final class Mongo2Env(val db: DB[Collection], settings: Settings) {

  import settings._
}
