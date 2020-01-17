package lila.clas

import play.api.Configuration
import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    messageApi: lila.message.MessageApi,
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.DataForm,
    authenticator: lila.user.Authenticator,
    baseUrl: BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val inviteSecret = appConfig.get[Secret]("class.invite.secret")

  lazy val forms = wire[ClasForm]

  private val colls = wire[ClasColls]

  lazy val api = wire[ClasApi]
}

private class ClasColls(db: lila.db.Db) {
  val teacher = db(CollName("clas_teacher"))
  val clas    = db(CollName("clas_clas"))
  val student = db(CollName("clas_student"))
}
