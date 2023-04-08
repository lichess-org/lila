package lila.simul

export lila.Lila.{ *, given }

private val logger = lila.log("simul")

case class SimulTeam(id: String, name: String, isIn: Boolean)
