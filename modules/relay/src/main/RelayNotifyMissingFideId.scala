package lila.relay

import lila.study.{ ChapterPreview, ChapterPreviewApi }
import lila.core.irc.IrcApi

private final class RelayNotifyMissingFideIds(api: RelayApi, irc: IrcApi, previewApi: ChapterPreviewApi)(using
    ex: Executor,
    scheduler: Scheduler
):
  private val once = scalalib.cache.OnceEvery[RelayRoundId](1 hour)

  def schedule(id: RelayRoundId) =
    if once(id) then
      scheduler.scheduleOnce(1 minute):
        api.byIdWithTour(id).flatMapz(checkNow)

  private def checkNow(rt: RelayRound.WithTour): Funit =
    previewApi
      .dataList(rt.round.studyId)
      .flatMap: chapters =>
        val missing: List[(StudyChapterId, String)] = chapters.flatMap: chapter =>
          chapter.players
            .so(_.toList)
            .find(_.fideId.isEmpty)
            .map: player =>
              (chapter.id, player.name.fold("?")(_.value))
        missing.nonEmpty.so:
          irc.broadcastMissingFideId(rt.round.id, rt.fullName, missing)
