package lila.coach

export lila.Lila.{ *, given }

private val logger = lila.log("coach")

import lila.user.Flag
opaque type CountrySelection = List[(Flag.Code, Flag.Name)]
object CountrySelection extends TotalWrapper[CountrySelection, List[(Flag.Code, Flag.Name)]]
