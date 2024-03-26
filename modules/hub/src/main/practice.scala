package lila.hub
package practice

case class OnComplete(userId: UserId, studyId: StudyId, chapterId: StudyChapterId)

trait Study:
  val id: StudyId
  val name: String
  val slug = lila.common.String.slugify(name)

type Studies    = StudyId => Option[Study]
type GetStudies = () => Fu[Studies]
