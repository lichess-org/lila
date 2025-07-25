package lila.core
package practice

import lila.core.id.{ StudyChapterId, StudyId }
import lila.core.study.data.StudyName
import lila.core.userId.UserId

case class OnComplete(userId: UserId, studyId: StudyId, chapterId: StudyChapterId)

trait Study:
  val id: StudyId
  val name: StudyName
  def slug: String

type Studies = StudyId => Option[Study]
type GetStudies = () => Fu[Studies]
