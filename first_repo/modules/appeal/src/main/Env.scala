package lila.appeal

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val coll = db(CollName("appeal"))

  lazy val forms = wire[AppealForm]

  lazy val api: AppealApi = wire[AppealApi]
}
