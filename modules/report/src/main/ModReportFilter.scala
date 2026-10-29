package lila.report

final class ModReportFilter:

  // mutable storage, because I cba to put it in DB
  private var modIdFilter = Map.empty[MyId, Option[Room]]

  def get(using mod: MyId): Option[Room] = modIdFilter.get(mod).flatten

  def set(filter: Option[Room])(using mod: MyId) =
    modIdFilter = modIdFilter + (mod -> filter)
