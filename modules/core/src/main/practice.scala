package lila.core
package practice

case class OnComplete(userId: UserId, studyId: StudyId, chapterId: StudyChapterId)

trait Study:
  val id: StudyId
  val name: StudyName
  def slug: String

type Studies    = StudyId => Option[Study]
type GetStudies = () => Fu[Studies]
