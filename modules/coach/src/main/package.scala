package lila.coach

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("coach")

import lila.core.user.Flag
opaque type CountrySelection = List[(Flag.Code, Flag.Name)]
object CountrySelection extends TotalWrapper[CountrySelection, List[(Flag.Code, Flag.Name)]]
