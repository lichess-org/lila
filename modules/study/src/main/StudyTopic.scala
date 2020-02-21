package lila.study

case class StudyTopic(value: String) extends AnyVal

object StudyTopic {

  implicit val topicIso = lila.common.Iso.string[StudyTopic](StudyTopic.apply, _.value)
}

case class StudyTopics(value: List[StudyTopic]) extends AnyVal

object StudyTopics {

  val empty = StudyTopics(Nil)

  def fromStrs(strs: List[String]) = StudyTopics {
    strs.view
      .map(_.trim)
      .filter(t => t.size >= 2 && t.size <= 50)
      .take(30)
      .map(StudyTopic.apply)
      .toList
      .distinct
  }
}
