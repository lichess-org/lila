package lila.user

import reactivemongo.api.bson.BSONHandler
import lila.db.dsl.*

enum UserMark:
  case Boost
  case Engine
  case Troll
  case Reportban
  case Rankban
  case ArenaBan
  case PrizeBan
  case Alt
  def key = toString.toLowerCase

object UserMark:
  val byKey: Map[String, UserMark] = values.mapBy(_.key)
  val bannable: Set[UserMark]      = Set(Boost, Engine, Troll, Alt)
  given BSONHandler[UserMark]      = valueMapHandler(byKey)(_.key)

opaque type UserMarks = List[UserMark]
object UserMarks extends TotalWrapper[UserMarks, List[UserMark]]:

  extension (a: UserMarks)
    def has(mark: UserMark): Boolean = a.value contains mark
    def boost: Boolean               = has(UserMark.Boost)
    def engine: Boolean              = has(UserMark.Engine)
    def troll: Boolean               = has(UserMark.Troll)
    def reportban: Boolean           = has(UserMark.Reportban)
    def rankban: Boolean             = has(UserMark.Rankban)
    def prizeban: Boolean            = has(UserMark.PrizeBan)
    def arenaBan: Boolean            = has(UserMark.ArenaBan)
    def alt: Boolean                 = has(UserMark.Alt)

    def nonEmpty   = a.value.nonEmpty option a
    def dirty      = a.value.exists(UserMark.bannable.contains)
    def clean      = !a.dirty
    def anyVisible = a.boost || a.engine

    def set(sel: UserMark.type => UserMark, v: Boolean) = UserMarks {
      if v then sel(UserMark) :: a.value
      else a.value.filter(sel(UserMark) !=)
    }

  val empty: UserMarks = Nil
