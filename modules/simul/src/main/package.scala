package lila

package object simul extends PackageObject {

  private[simul] val logger = lila.log("simul")

}

package simul {
  case class SimulTeam(id: String, name: String, isIn: Boolean)
}
