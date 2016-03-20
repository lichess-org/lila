package lila

package object team extends PackageObject with WithPlay {

  object tube {

    implicit lazy val teamTube = Team.tube inColl Env.current.teamColl

    private[team] implicit lazy val requestTube = Request.tube inColl Env.current.requestColl

    private[team] implicit lazy val memberTube = Member.tube inColl Env.current.memberColl
  }

  private[team] def logger = lila.log("team")
}
