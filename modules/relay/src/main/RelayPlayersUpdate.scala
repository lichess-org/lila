package lila.relay

import lila.study.{ Chapter, ChapterRepo, StudyApi }
import lila.core.socket.Sri
import akka.stream.scaladsl.Sink

/* When the players replacement text of a tournament is updated,
 * we go through all rounds of the tournament and immediately apply
 * the player replacements to all games.
 * Then we enrich all affected games based on the potentially new FIDE ID
 * of each player. */
private final class RelayPlayersUpdate(
    roundRepo: RelayRoundRepo,
    fidePlayerApi: RelayFidePlayerApi,
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
)(using Executor, akka.stream.Materializer):

  def apply(tour: RelayTour, prev: RelayTour): Funit =
    tour.players.so:
      _.parse
        .diff(prev.players.map(_.parse))
        .so: newPlayers =>
          val enrichFromFideId = fidePlayerApi.enrichTags(tour)
          for
            studyIds <- roundRepo.studyIdsOf(tour.id)
            _ <- chapterRepo
              .byStudiesSource(studyIds)
              .mapAsync(1): chapter =>
                val (newTags, _) = newPlayers.update(chapter.tags)
                (newTags != chapter.tags).so:
                  enrichFromFideId(newTags)
                    .flatMap: enriched =>
                      val newName = Chapter.nameFromPlayerTags(enriched)
                      studyApi.setTagsAndRename(
                        studyId = chapter.studyId,
                        chapterId = chapter.id,
                        tags = enriched,
                        newName = newName.filter(_ != chapter.name)
                      )(lila.study.actorApi.Who(chapter.ownerId, Sri("")))
              .runWith(Sink.ignore)
          yield ()
