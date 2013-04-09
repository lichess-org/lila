package lila.app

trait Modules {

  def userEnv = lila.user.Env.current
  def gameEnv = lila.game.Env.current
}
