package lila
package ai

trait Server {

  val levelRange = 1 to 8

  def validateLevel(level: Int) = 
    level.validIf(levelRange contains level, "Invalid AI level: " + level)
}
