package lila.core
package practice

import lila.core.id.{ StudyId, StudyChapterId }
import lila.core.study.StudyName
import lila.core.userId.UserId

case class OnComplete(userId: UserId, studyId: StudyId, chapterId: StudyChapterId)

trait Study:
  val id: StudyId
  val name: lila.core.study.StudyName
  def slug: String

type Studies    = StudyId => Option[Study]
type GetStudies = () => Fu[Studies]
