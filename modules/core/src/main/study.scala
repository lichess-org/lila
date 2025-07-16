package lila.core
package study

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.id.StudyId
import lila.core.userId.UserId

object data:

  opaque type StudyName = String
  object StudyName extends OpaqueString[StudyName]

  opaque type StudyChapterName = String
  object StudyChapterName extends OpaqueString[StudyChapterName]

import data.*

case class IdName(@Key("_id") id: StudyId, name: StudyName)

trait Study:
  def id: StudyId
  def ownerId: UserId
  def visibility: Visibility

enum Visibility:
  case `private`, unlisted, public
  def key = toString
object Visibility:
  val byKey = values.mapBy(_.key)

trait StudyApi:
  def publicIdNames(ids: List[StudyId]): Fu[List[IdName]]
  def byId(id: StudyId): Fu[Option[Study]]

case class StartStudy(studyId: StudyId)
case class RemoveStudy(studyId: StudyId)

enum Order:
  case hot, newest, oldest, updated, popular, alphabetical, mine

case class GetRelayCrowd(id: StudyId, promise: Promise[Int])
