package lila.clas

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo,
    historyApi: lila.history.HistoryApi,
    messageApi: lila.message.MessageApi,
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.DataForm,
    authenticator: lila.user.Authenticator,
    cacheApi: lila.memo.CacheApi,
    baseUrl: BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  lazy val nameGenerator = wire[NameGenerator]

  lazy val forms = wire[ClasForm]

  private val colls = wire[ClasColls]

  lazy val api: ClasApi = wire[ClasApi]

  private def getStudentIds = () => api.student.allIds

  lazy val progressApi = wire[ClasProgressApi]

  lila.common.Bus.subscribeFun("finishGame") {
    case lila.game.actorApi.FinishGame(game, _, _) => progressApi.onFinishGame(game)
  }
}

private class ClasColls(db: lila.db.Db) {
  val teacher = db(CollName("clas_teacher"))
  val clas    = db(CollName("clas_clas"))
  val student = db(CollName("clas_student"))
}
