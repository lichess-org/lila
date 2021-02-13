package lila.clas

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo,
    historyApi: lila.history.HistoryApi,
    puzzleColls: lila.puzzle.PuzzleColls,
    msgApi: lila.msg.MsgApi,
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.SecurityForm,
    authenticator: lila.user.Authenticator,
    cacheApi: lila.memo.CacheApi,
    baseUrl: BaseUrl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
) {

  lazy val nameGenerator: NameGenerator = wire[NameGenerator]

  lazy val forms = wire[ClasForm]

  private val colls = wire[ClasColls]

  lazy val studentCache = wire[ClasStudentCache]

  lazy val api: ClasApi = wire[ClasApi]

  lazy val progressApi = wire[ClasProgressApi]

  lazy val markup = wire[ClasMarkup]

  lila.common.Bus.subscribeFuns(
    "finishGame" -> { case lila.game.actorApi.FinishGame(game, _, _) =>
      progressApi.onFinishGame(game).unit
    },
    "clas" -> { case lila.hub.actorApi.clas.IsTeacherOf(teacher, student, promise) =>
      promise completeWith api.clas.isTeacherOfStudent(teacher, Student.Id(student))
    }
  )
}

private class ClasColls(db: lila.db.Db) {
  val clas    = db(CollName("clas_clas"))
  val student = db(CollName("clas_student"))
  val invite  = db(CollName("clas_invite"))
}
