package lila.study

case class StudyTopic(value: String) extends AnyVal

object StudyTopic {

  implicit val topicIso = lila.common.Iso.string[StudyTopic](StudyTopic.apply, _.value)
}

case class StudyTopics(value: List[StudyTopic]) extends AnyVal

object StudyTopics {

  val empty = StudyTopics(Nil)

  def fromStr(str: String) = StudyTopics {
    str
      .split(' ')
      .view
      .map(_.trim)
      .filter(t => t.size >= 2 && t.size <= 40)
      .take(20)
      .map(StudyTopic.apply)
      .toList
  }
}
