package lila.clas

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._
import lila.security.Permission

@Module
final class Env(
    userRepo: lila.user.UserRepo,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val teacherColl = db(CollName("clas_teacher"))
  private lazy val clasColl    = db(CollName("clas_clas"))

  lazy val api = new ClasApi(
    teacherColl = teacherColl,
    clasColl = clasColl,
    userRepo = userRepo
  )
}
