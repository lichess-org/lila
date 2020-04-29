package lila.swiss

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val colls = wire[SwissColls]

  val api = wire[SwissApi]

  lazy val forms = wire[SwissForm]
}

private class SwissColls(db: lila.db.Db) {
  val swiss = db(CollName("swiss"))
  val round = db(CollName("swiss_round"))
}
