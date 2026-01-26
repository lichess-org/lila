package lila.clas

import com.softwaremill.macwire.*

import lila.core.config.*
import lila.core.misc.clas.ClasBus

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    gameRepo: lila.core.game.GameRepo,
    historyApi: lila.core.history.HistoryApi,
    puzzleColls: lila.puzzle.PuzzleColls,
    msgApi: lila.core.msg.MsgApi,
    lightUserAsync: lila.core.LightUser.Getter,
    signupForm: lila.core.security.SignupForm,
    authenticator: lila.core.security.Authenticator,
    cacheApi: lila.memo.CacheApi,
    markdownCache: lila.memo.MarkdownCache,
    hcaptcha: lila.core.security.Hcaptcha,
    baseUrl: BaseUrl
)(using Executor, Scheduler, akka.stream.Materializer, lila.core.i18n.Translator, play.api.Mode):

  lazy val nameGenerator: NameGenerator = wire[NameGenerator]

  lazy val forms = wire[ClasForm]

  private val colls = wire[ClasColls]

  lazy val filters = wire[ClasUserFilters]

  lazy val mates = wire[ClasMates]

  lazy val api: ClasApi = wire[ClasApi]

  lazy val progressApi = wire[ClasProgressApi]

  lazy val markdown = wire[ClasMarkdown]

  lazy val login = wire[ClasLoginApi]

  lazy val bulk = wire[ClasBulkApi]

  def isTeacher(using me: Me) =
    lila.core.perm.Granter(_.Teacher) && filters.teacher(me)

  def hasClas(using me: Me) =
    filters.student(me) || isTeacher

  lila.common.Bus.sub[lila.core.game.FinishGame]: finish =>
    progressApi.onFinishGame(finish.game)

  lila.common.Bus.sub[ClasBus]:
    case ClasBus.IsTeacherOf(teacher, student, promise) =>
      promise.completeWith(api.clas.isTeacherOf(teacher, student))
    case ClasBus.CanKidsUseMessages(kid1, kid2, promise) =>
      promise.completeWith(api.clas.canKidsUseMessages(kid1, kid2))
    case ClasBus.ClasMatesAndTeachers(kid, promise) =>
      promise.completeWith(mates.get(kid.id))

private final class ClasColls(db: lila.db.Db):
  val clas = db(CollName("clas_clas"))
  val student = db(CollName("clas_student"))
  val invite = db(CollName("clas_invite"))
  val login = db(CollName("clas_login"))
