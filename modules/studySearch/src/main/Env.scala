package lila.studySearch

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.*

import akka.actor.Cancellable

import com.softwaremill.macwire.*

import lila.common.Lilakka
import lila.search.SearchClient
import lila.core.study.StudyOrder
import lila.core.study.IndexStudySearch
import lila.core.study.RemoveStudy
import lila.core.id.StudyId

final class Env(
    studyRepo: lila.study.StudyRepo,
    pager: lila.study.StudyPager,
    elastic: SearchClient,
    shutdown: akka.actor.CoordinatedShutdown
)(using Executor)(using scheduler: Scheduler):

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(text: String, order: StudyOrder, page: Int)(using me: Option[Me]) =
    val query =
      StudySearchApi.Query(
        text.take(100),
        order.toSorting,
        me.map(_.userId.value)
      )
    lila.search
      .PaginatorBuilder(api, pager.maxPerPage)(query, page)
      .flatMap(_.mapFutureList(pager.withChaptersAndLiking()))
      .dmap(_.toPaginator)

  extension (order: StudyOrder)
    def toSorting: Option[StudySearchApi.Sorting] =
      import StudySearchApi.{ Field, Order, Sorting }
      order.match
        case StudyOrder.alphabetical => Sorting(Field.Name, Order.Asc).some
        case StudyOrder.hot => Sorting(Field.Hot, Order.Desc).some
        case StudyOrder.newest => Sorting(Field.CreatedAt, Order.Desc).some
        case StudyOrder.oldest => Sorting(Field.CreatedAt, Order.Asc).some
        case StudyOrder.popular => Sorting(Field.Likes, Order.Desc).some
        case StudyOrder.updated => Sorting(Field.UpdatedAt, Order.Desc).some
        case StudyOrder.mine => Sorting(Field.Likes, Order.Asc).some
        case StudyOrder.relevant => none

  private val pendingUpserts = ConcurrentHashMap[StudyId, Cancellable]()
  private val shuttingDown = AtomicBoolean(false)
  private val perStudyUpdateCadence = 5.minutes

  lila.common.Bus.sub[RemoveStudy]: event =>
    Option(pendingUpserts.remove(event.studyId)).so(_.cancel())
    elastic.delete(SearchClient.Index.Study, event.studyId)

  lila.common.Bus.sub[IndexStudySearch]: event =>
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

  Lilakka.shutdown(shutdown, _.PhaseBeforeServiceUnbind, "Flush study search updates"): () =>
    shuttingDown.set(true)
    pendingUpserts.keys.asScala.toList.sequentiallyVoid(flushPendingUpsert)

  private def flushPendingUpsert(studyId: StudyId): Funit =
    Option(pendingUpserts.remove(studyId)).fold(funit): scheduled =>
      scheduled.cancel()
      elastic.upsert(SearchClient.Index.Study, studyId)
