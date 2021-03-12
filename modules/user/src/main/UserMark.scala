package lila.user

import reactivemongo.api.bson.BSONHandler
import lila.db.dsl._

sealed trait UserMark {
  def key = toString.toLowerCase
}
object UserMark {
  case object Boost     extends UserMark
  case object Engine    extends UserMark
  case object Troll     extends UserMark
  case object Reportban extends UserMark
  case object Rankban   extends UserMark
  case object Alt       extends UserMark
  val all = List(Boost, Engine, Troll, Reportban, Rankban, Alt)
  val indexed: Map[String, UserMark] = all.view.map { m =>
    m.key -> m
  }.toMap
  val bannable: Set[UserMark]  = Set(Boost, Engine, Troll, Alt)
  implicit val markBsonHandler = stringAnyValHandler[UserMark](_.key, indexed.apply)
}

case class UserMarks(value: List[UserMark]) extends AnyVal {
  def apply(mark: UserMark) = value contains mark
  def boost                 = apply(UserMark.Boost)
  def engine                = apply(UserMark.Engine)
  def troll                 = apply(UserMark.Troll)
  def reportban             = apply(UserMark.Reportban)
  def rankban               = apply(UserMark.Rankban)
  def alt                   = apply(UserMark.Alt)

  def nonEmpty = value.nonEmpty option this
  def clean    = !value.exists(UserMark.bannable.contains)

  def set(sel: UserMark.type => UserMark, v: Boolean) =
    UserMarks {
      if (v) sel(UserMark) :: value
      else value.filter(sel(UserMark) !=)
    }
}
object UserMarks {
  val empty = UserMarks(Nil)

  implicit val marksBsonHandler =
    implicitly[BSONHandler[List[UserMark]]].as[UserMarks](UserMarks.apply, _.value.distinct)
}
