package lila.report

import lila.user.User

final class ModReportFilter {

  // mutable storage, because I cba to put it in DB
  private var modIdFilter = Map.empty[User.ID, Option[Room]]

  def get(mod: User): Option[Room] = modIdFilter.get(mod.id).flatten

  def set(mod: User, filter: Option[Room]) =
    modIdFilter = modIdFilter + (mod.id -> filter)
}
