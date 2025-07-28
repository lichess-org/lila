package lila.recap

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("recap")

private val yearToRecap = 2024
private val dateStart = instantOf(yearToRecap, 1, 1, 0, 0)
private val dateEnd = instantOf(yearToRecap + 1, 1, 1, 0, 0)
