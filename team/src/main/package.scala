package lila

package object team extends PackageObject with WithPlay {

  // expose team tube
  lazy val teamTube = Teams.tube inColl Env.current.teamColl

  private[team] lazy val requestTube = Requests.tube inColl Env.current.requestColl

  private[team] lazy val memberTube = Members.tube inColl Env.current.memberColl

  private[team] object allTubes {
    implicit def teamT = teamTube
    implicit def requestT = requestTube
    implicit def memberT = memberTube
  }
}
