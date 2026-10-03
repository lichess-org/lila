package lila.relay

import com.github.blemale.scaffeine.Cache

import lila.study.ChapterPreviewApi
import lila.core.irc.IrcApi
import lila.core.userId.ModId
import lila.core.data.DiffStr

private final class RelayNotifierAdmin(roundRepo: RelayRoundRepo, irc: IrcApi, previewApi: ChapterPreviewApi)(
    using
    ex: Executor,
    scheduler: Scheduler
):

  object orphanBoards:

    private val notifyAfterMisses = 10

    private val counter: Cache[StudyChapterId, Int] = scalalib.cache.scaffeineNoScheduler
      .expireAfterWrite(3.minutes)
      .build[StudyChapterId, Int]()

    private val once = scalalib.cache.OnceEvery[StudyChapterId](4.hour)

    def inspectPlan(rt: RelayRound.WithTour, plan: RelayUpdatePlan.Plan): Funit = Future:
      rt.tour.tier.foreach: tier =>
        if plan.input.games.nonEmpty && !rt.round.sync.upstream.exists(_.isInternal)
        then
          counter.invalidateAll(plan.update.map(_._1.id))
          plan.orphans.foreach: chapter =>
            val count = ~counter.getIfPresent(chapter.id) + 1
            if rt.tour.orphanWarn && count >= notifyAfterMisses && once(chapter.id)
            then irc.broadcastOrphanBoard(rt.round.id, rt.fullNameNoTrans, chapter.id, chapter.name, tier.key)
            else counter.put(chapter.id, count)

  object tooManyGames:

    private val once = scalalib.cache.OnceEvery[RelayRoundId](1.hour)

    def apply(rt: RelayRound.WithTour, games: Int, max: Max): Funit =
      once(rt.round.id).so:
        irc.broadcastError(
          rt.round.id,
          rt.fullNameNoTrans,
          s"Too many games from source: $games. Max is $max"
        )

  object missingFideIds:
    private val once = scalalib.cache.OnceEvery[RelayRoundId](1.hour)

    def schedule(id: RelayRoundId) =
      if once(id) then
        scheduler.scheduleOnce(1.minute):
          roundRepo.byIdWithTour(id).flatMapz(checkNow)

    private def checkNow(rt: RelayRound.WithTour): Funit =
      if rt.round.sync.upstream.exists(_.isInternal)
      then funit
      else
        previewApi
          .dataList(rt.round.studyId)
          .flatMap: chapters =>
            val missing: List[(StudyChapterId, String)] = chapters.flatMap: chapter =>
              chapter.players
                .so(_.toList)
                .filter(_.fideId.isEmpty)
                .map: player =>
                  (chapter.id, player.name.fold("?")(_.value))
            missing.nonEmpty.so:
              irc.broadcastMissingFideId(rt.round.id, rt.fullNameNoTrans, missing)

  def tourCreate(tour: RelayTour)(using Me): Funit =
    tour.official.so:
      val diff = DiffStr(s"+ tier: ${tour.tier.fold("(none)")(_.toString)}")
      irc.broadcastTourUpdate(tour.name.value, tour.slug, tour.id, diff)

  def tourChange(prev: RelayTour, tour: RelayTour, impersonatedBy: Option[ModId])(using Me): Funit =
    lila.common
      .ProductDiff(
        prev.some,
        tour,
        ignoredFields = Set("id", "createdAt", "active", "live", "syncedAt", "note"),
        nestedFields = Set("info", "spotlight"),
        maxLength = 300
      )
      .so(irc.broadcastTourUpdate(tour.name.value, tour.slug, tour.id, _, impersonatedBy))

  def imageDelete(t: RelayTour, tag: Option[String], impersonatedBy: Option[ModId])(using Me): Funit =
    t.official.so:
      val fieldName = tag | "image"
      val diff = DiffStr(s"- $fieldName: ${t.image.fold("(none)")(_.toString)}\n+ $fieldName: (removed)")
      irc.broadcastTourUpdate(t.name.value, t.slug, t.id, diff, impersonatedBy)

  def imageUpload(t: RelayTour, tag: Option[String], impersonatedBy: Option[ModId])(using Me): Funit =
    t.official.so:
      val fieldName = tag | "image"
      val diff = DiffStr(s"- $fieldName: ${t.image.fold("(none)")(_.toString)}\n+ $fieldName: (uploaded)")
      irc.broadcastTourUpdate(t.name.value, t.slug, t.id, diff, impersonatedBy)
