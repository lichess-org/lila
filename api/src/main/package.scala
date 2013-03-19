package lila

package object api extends PackageObject with WithPlay {

  lazy val env = new Env(
    application = play.api.Play.current,
    config = lila.common.PlayApp.loadConfig
  )
}
