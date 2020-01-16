package lila.clas

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val teacherColl = db(CollName("clas_teacher"))
  private lazy val clasColl    = db(CollName("clas_clas"))

  lazy val forms = wire[ClasForm]

  lazy val api = new ClasApi(
    teacherColl = teacherColl,
    clasColl = clasColl
  )
}
