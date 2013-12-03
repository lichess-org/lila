package lila

package object user extends PackageObject with WithPlay {

  object tube {

    // expose user tube
    implicit lazy val userTube = User.tube inColl Env.current.userColl

    private[user] implicit lazy val speedElosTube = SpeedElos.tube
    private[user] implicit lazy val variantElosTube = VariantElos.tube
    private[user] implicit lazy val profileTube = Profile.tube

    private[user] implicit lazy val historyTube =
      lila.db.JsTube.json inColl Env.current.historyColl
  }
}
