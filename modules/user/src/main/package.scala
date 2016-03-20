package lila

package object user extends PackageObject with WithPlay {

  object tube {

    // expose user tube
    implicit lazy val userTube = User.tube inColl Env.current.userColl
  }

  private[user] def logger = lila.log("user")

  type Trophies = List[Trophy]
}
