package lila.db

import reactivemongo.api.DB

final class DbEnv(db: DB) {

  def apply(name: String): ReactiveColl = db(name)
}
