package lila.tournament

import lila.core.user.FlagCode

private object ForbiddenFlag:

  private val list: Map[TourId, List[FlagCode]] = Map(
    TourId("fscday25") -> List(FlagCode("RU"), FlagCode("BY"), FlagCode("FR"))
  )

  def isForbidden(tourId: TourId)(using me: Me): Boolean =
    me.profile
      .flatMap(_.flag)
      .exists: myFlag =>
        list.get(tourId).exists(_.contains(myFlag))

  def isForbiddenSomewhere(flag: FlagCode): List[TourId] =
    list.collect:
      case (tourId, forbiddenFlags) if forbiddenFlags.contains(flag) => tourId
