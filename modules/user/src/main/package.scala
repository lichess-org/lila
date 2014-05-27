package lila

package object user extends PackageObject with WithPlay {

  object tube {

    // expose user tube
    implicit lazy val userTube = User.tube inColl Env.current.userColl

    private[user] implicit lazy val profileTube = Profile.tube

    private[user] implicit lazy val historyColl = Env.current.historyColl
  }

  private[user] def maxInactivityDate = org.joda.time.DateTime.now minusMonths 6
}
