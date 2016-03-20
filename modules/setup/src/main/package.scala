package lila

import lila.socket.WithSocket

package object setup extends PackageObject with WithPlay with WithSocket {

  object tube {

    private[setup] implicit lazy val userConfigTube =
      UserConfig.tube inColl Env.current.userConfigColl

    private[setup] implicit lazy val anonConfigTube =
      UserConfig.tube inColl Env.current.anonConfigColl
  }

  private[setup] def logger = lila.log("setup")
}
