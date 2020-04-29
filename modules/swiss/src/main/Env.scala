package lila.clas

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val colls = wire[SwissColls]

}

private class SwissColls(db: lila.db.Db) {
  val swiss = db(CollName("swiss"))
}
