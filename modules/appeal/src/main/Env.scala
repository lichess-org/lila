package lila.appeal

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    noteApi: lila.user.NoteApi,
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val coll = db(CollName("appeal"))

  private lazy val snoozer = new lila.memo.Snoozer[Appeal.SnoozeKey](cacheApi)

  lazy val api: AppealApi = wire[AppealApi]
}
