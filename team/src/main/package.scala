package lila

package object team extends PackageObject with WithPlay {

  private[team] lazy val teamTube = Teams.tube inColl Env.current.teamColl
}
