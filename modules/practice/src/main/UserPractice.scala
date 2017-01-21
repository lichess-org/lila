package lila.practice

import lila.study.{ Study, Chapter }

case class UserPractice(
    progress: PracticeProgress,
    structure: PracticeStructure,
    chapters: Map[Study.Id, Vector[Chapter.IdName]]) {

  import UserPractice._

  // def progress(studyId: Study.Id) = Progress(
  //   done = 

}

object UserPractice {

  case class Progress(done: Int, total: Int) {

    def percent = if (total == 0) 0 else done * 100 / total
  }
}
