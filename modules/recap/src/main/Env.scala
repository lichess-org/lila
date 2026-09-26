package lila.recap

import com.softwaremill.macwire.*

import lila.core.config.CollName
import lila.db.dsl.{ *, given }
import lila.core.i18n.{ LangPicker, Translator, I18nKey }

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    puzzleColls: lila.puzzle.PuzzleColls,
    lightUserApi: lila.core.user.LightUserApi,
    userApi: lila.user.UserApi,
    langPicker: LangPicker,
    settingStore: lila.memo.SettingStore.Builder
)(using Translator, Executor, Scheduler, org.apache.pekko.stream.Materializer, play.api.Mode):

  lazy val parallelismSetting = settingStore[Int](
    "recapParallelism",
    default = 8,
    text = "Number of yearly recaps to build in parallel".some
  )

  private val colls = RecapColls(db(CollName("recap_report")), db(CollName("recap_queue")))

  private val json = wire[RecapJson]

  private val repo = wire[RecapRepo]

  private val builder = wire[RecapBuilder]

  private val queue = ParallelMongoQueue[UserId](
    coll = colls.queue,
    parallelism = () => parallelismSetting.get(),
    computationTimeout = 2.minutes,
    name = "recap"
  ): uid =>
    builder.compute(uid)

  lazy val api = wire[RecapApi]

  def translateNotif(userId: UserId, year: String): Fu[(String, String)] = for
    lang <- userApi.langOf(userId).map(langPicker.byLangTagOrDefault)
    given play.api.i18n.Lang = lang
    title = I18nKey.recap.recapReady.txt(year)
    body = I18nKey.recap.awaitQuestion.txt()
  yield (title, body)

final private class RecapColls(val recap: Coll, val queue: Coll)
