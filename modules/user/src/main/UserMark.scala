package lila.user

import reactivemongo.api.bson.BSONHandler

import lila.db.dsl.*
import lila.core.user.{ UserMark, UserMarks }

object UserMarkExtensions:
  extension (a: UserMarks)
    def hasMark(mark: UserMark): Boolean = a.value contains mark
    def boost: Boolean                   = hasMark(UserMark.boost)
    def engine: Boolean                  = hasMark(UserMark.engine)
    def troll: Boolean                   = hasMark(UserMark.troll)
    def reportban: Boolean               = hasMark(UserMark.reportban)
    def rankban: Boolean                 = hasMark(UserMark.rankban)
    def prizeban: Boolean                = hasMark(UserMark.prizeBan)
    def arenaBan: Boolean                = hasMark(UserMark.arenaBan)
    def alt: Boolean                     = hasMark(UserMark.alt)

    def nonEmpty   = a.value.nonEmpty.option(a)
    def dirty      = a.value.exists(UserMark.bannable.contains)
    def clean      = !a.dirty
    def anyVisible = a.boost || a.engine

    def set(sel: UserMark.type => UserMark, v: Boolean) = lila.core.user.UserMarks {
      if v then sel(UserMark) :: a.value
      else a.value.filter(sel(UserMark) !=)
    }
