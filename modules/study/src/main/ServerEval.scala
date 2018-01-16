package lila.study

import lila.analyse.Analysis

final class ServerEval(
    socketHub: akka.actor.ActorRef,
    chapterRepo: ChapterRepo
) {

  def progress(analysis: Analysis): Unit = {
    studySocket ! Tell(analysis.id, actorApi.StudyAnalysisProgress(analysis))
  }
}
