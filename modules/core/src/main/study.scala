package lila.core
package study

import reactivemongo.api.bson.Macros.Annotations.Key

case class IdName(@Key("_id") id: StudyId, name: StudyName)

trait Study:
  def id: StudyId
  def ownerId: UserId
  def visibility: Visibility

enum Visibility:
  case `private`, unlisted, public
object Visibility:
  val byKey = values.mapBy(_.toString)

trait StudyApi:
  def publicIdNames(ids: List[StudyId]): Fu[List[IdName]]
  def byId(id: StudyId): Fu[Option[Study]]

case class StartStudy(studyId: StudyId)
case class RemoveStudy(studyId: StudyId)
