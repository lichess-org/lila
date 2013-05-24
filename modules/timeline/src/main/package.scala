package lila

package object timeline extends PackageObject with WithPlay {

  object tube {

    private[timeline] implicit lazy val gameEntryTube =
      GameEntry.tube inColl Env.current.gameEntryColl
  }
}
