package lila

import lila.socket.WithSocket

package object setup extends PackageObject with WithPlay with WithSocket {

  object tube {

    private[setup] implicit lazy val aiConfigTube = AiConfig.tube

    private[setup] implicit lazy val friendConfigTube = FriendConfig.tube

    private[setup] implicit lazy val hookConfigTube = HookConfig.tube

    private[setup] implicit lazy val filterConfigTube = FilterConfig.tube

    private[setup] implicit lazy val userConfigTube = 
      UserConfig.tube inColl Env.current.userConfigColl

    private[setup] implicit lazy val anonConfigTube = 
      UserConfig.tube inColl Env.current.anonConfigColl
  }
}
