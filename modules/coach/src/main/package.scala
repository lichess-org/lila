package lila.coach

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
import lila.core.user.{ FlagCode, FlagName }

private val logger = lila.log("coach")

val allFlags = FlagCode("all")

opaque type CountrySelection = List[(FlagCode, FlagName)]
object CountrySelection extends TotalWrapper[CountrySelection, List[(FlagCode, FlagName)]]
