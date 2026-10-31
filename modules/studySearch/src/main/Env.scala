package lila.studySearch

import com.softwaremill.macwire.*
import org.apache.pekko.actor.Cancellable

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.*

import lila.common.Lilakka
import lila.core.id.StudyId
import lila.core.study.{ IndexStudySearch, RemoveStudy, StudyOrder }
import lila.search.SearchClient
import lila.study.ui.StudyFormat

final class Env(
    studyRepo: lila.study.StudyRepo,
    pager: lila.study.StudyPager,
    elastic: SearchClient,
    shutdown: org.apache.pekko.actor.CoordinatedShutdown
)(using Executor)(using scheduler: Scheduler):

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(text: String, order: StudyOrder, page: Int)(using me: Option[Me])(using StudyFormat) =
    val query =
      StudySearchApi.Query(
        text.take(100),
        StudySearchApi.Sorting.fromStudyOrder(order),
        me.map(_.userId.value)
      )
    lila.search
      .PaginatorBuilder(api, pager.maxPerPage)(query, page)
      .flatMap(_.mapFutureList(pager.withChaptersAndLiking()))
      .dmap(_.toPaginator)

  private val pendingUpserts = ConcurrentHashMap[StudyId, Cancellable]()
  private val shuttingDown = AtomicBoolean(false)
  private val perStudyUpdateCadence = 5.minutes

  lila.common.Bus.sub[RemoveStudy]: event =>
    Option(pendingUpserts.remove(event.studyId)).so(_.cancel())
    elastic.delete(SearchClient.Index.Study, event.studyId)

  lila.common.Bus.sub[IndexStudySearch]: event =>
    indexStudy(event).logFailure(lila.log("studySearch"))

  private def indexStudy(event: IndexStudySearch): Funit =
    if shuttingDown.get || event.now then
      Option(pendingUpserts.remove(event.studyId)).so(_.cancel())
      elastic.upsert(SearchClient.Index.Study, event.studyId)
    else
      val scheduled = pendingUpserts.computeIfAbsent(
        event.studyId,
        studyId =>
          scheduler.scheduleOnce(perStudyUpdateCadence):
            flushPendingUpsert(studyId)
      )
      if shuttingDown.get && pendingUpserts.remove(event.studyId, scheduled) then
        scheduled.cancel()
        elastic.upsert(SearchClient.Index.Study, event.studyId)
      else funit

  Lilakka.shutdown(shutdown, _.PhaseBeforeServiceUnbind, "Flush study search updates"): () =>
    shuttingDown.set(true)
    pendingUpserts.keys.asScala.toList.sequentiallyVoid(flushPendingUpsert)

  private def flushPendingUpsert(studyId: StudyId): Funit =
    Option(pendingUpserts.remove(studyId)).fold(funit): scheduled =>
      scheduled.cancel()
      elastic.upsert(SearchClient.Index.Study, studyId)
