package lila

package object wiki extends PackageObject with WithPlay with WithDb {

  lazy val env = new Env(
    config = lila.common.PlayApp.loadConfig,
    db = lila.db.env)
}
