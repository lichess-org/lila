package lila.report

import lila.user.Me

final class ModReportFilter:

  // mutable storage, because I cba to put it in DB
  private var modIdFilter = Map.empty[Me.Id, Option[Room]]

  def get(mod: Me): Option[Room] = modIdFilter.get(mod).flatten

  def set(mod: Me, filter: Option[Room]) =
    modIdFilter = modIdFilter + (mod.myId -> filter)
