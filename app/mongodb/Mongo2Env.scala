package lila
package mongodb

import reactivemongo.api._

import core.Settings

final class Mongo2Env(val db: DB[Collection], settings: Settings) {

  import settings._
}
