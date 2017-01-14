package lila.report

import lila.user.User

final class ModReportFilter {

  // mutable storage, because I cba to put it in DB
  var modIdFilter = Map.empty[User.ID, Option[Reason]]

  def get(mod: User): Option[Reason] = modIdFilter.get(mod.id).flatten

  def set(mod: User, filter: Option[Reason]) =
    modIdFilter = modIdFilter + (mod.id -> filter)
}
