package lila.practice

import lila.study.{ Study, Chapter }

case class UserPractice(
    structure: PracticeStructure,
    progress: PracticeProgress) {

  import UserPractice._

  def progressOn(studyId: Study.Id) = {
    val chapterIds = structure.study(studyId).??(_.chapterIds)
    Progress(
      done = progress countDone chapterIds,
      total = chapterIds.size)
  }

}

object UserPractice {

  case class Progress(done: Int, total: Int) {

    def percent = if (total == 0) 0 else done * 100 / total

    def complete = done >= total
  }
}
