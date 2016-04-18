package lila.study

case class Location(study: Study, chapterId: Chapter.ID, chapter: Chapter) {

  def withChapter(c: Chapter) = copy(chapter = c)
}

case object Location {

  case class Ref(studyId: Study.ID, chapterId: Chapter.ID) {

    def id = s"$studyId:$chapterId"
  }

  object Ref {

    type ID = String

    def parseId(str: ID): Option[Ref] = str.split(':') match {
      case Array(s, c) if s.size == Study.idSize && c.size == Chapter.idSize => Ref(s, c).some
      case _ => None
    }
  }
}
