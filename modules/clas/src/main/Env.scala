package lila.clas

import com.softwaremill.macwire.*

import lila.common.config.*

@Module
@annotation.nowarn("msg=unused")
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    gameRepo: lila.game.GameRepo,
    historyApi: lila.history.HistoryApi,
    puzzleColls: lila.puzzle.PuzzleColls,
    msgApi: lila.msg.MsgApi,
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.SecurityForm,
    authenticator: lila.user.Authenticator,
    cacheApi: lila.memo.CacheApi,
    baseUrl: BaseUrl
)(using Executor, Scheduler, akka.stream.Materializer, play.api.Mode):

  lazy val nameGenerator: NameGenerator = wire[NameGenerator]

  lazy val forms = wire[ClasForm]

  private val colls = wire[ClasColls]

  lazy val studentCache = wire[ClasStudentCache]

  lazy val matesCache = wire[ClasMatesCache]

  lazy val api: ClasApi = wire[ClasApi]

  lazy val progressApi = wire[ClasProgressApi]

  lazy val markup = wire[ClasMarkup]

  def hasClas(using me: lila.user.Me) =
    lila.security.Granter(_.Teacher) || studentCache.isStudent(me)

  lila.common.Bus.subscribeFuns(
    "finishGame" -> { case lila.game.actorApi.FinishGame(game, _) =>
      progressApi.onFinishGame(game)
    },
    "clas" -> {
      case lila.hub.actorApi.clas.IsTeacherOf(teacher, student, promise) =>
        promise completeWith api.clas.isTeacherOf(teacher, student)
      case lila.hub.actorApi.clas.AreKidsInSameClass(kid1, kid2, promise) =>
        promise completeWith api.clas.areKidsInSameClass(kid1, kid2)
      case lila.hub.actorApi.clas.ClasMatesAndTeachers(kid, promise) =>
        promise completeWith matesCache.get(kid.id)
    }
  )

private class ClasColls(db: lila.db.Db):
  val clas    = db(CollName("clas_clas"))
  val student = db(CollName("clas_student"))
  val invite  = db(CollName("clas_invite"))
