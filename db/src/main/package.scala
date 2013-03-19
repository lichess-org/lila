package lila

package object db extends PackageObject with WithPlay with db.Implicits {

  lazy val env = new Env(lila.common.PlayApp.loadConfig)
}
