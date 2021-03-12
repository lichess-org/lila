package lila.report

import lila.user.{ Holder, User }

final class ModReportFilter {

  // mutable storage, because I cba to put it in DB
  private var modIdFilter = Map.empty[User.ID, Option[Room]]

  def get(mod: Holder): Option[Room] = modIdFilter.get(mod.id).flatten

  def set(mod: Holder, filter: Option[Room]) =
    modIdFilter = modIdFilter + (mod.id -> filter)
}
