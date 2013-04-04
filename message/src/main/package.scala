package lila

package object message extends PackageObject with WithPlay {

  object tube {

    private[message] implicit lazy val threadTube =
      Thread.tube inColl Env.current.threadColl
  }
}
