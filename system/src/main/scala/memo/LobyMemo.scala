package lila.system
package memo

import scalaz.effects._

final class LobbyMemo {

  private var privateVersion: Int = 1

  def version: Int = privateVersion

  def ++ : IO[Unit] = io {
    privateVersion = privateVersion + 1
  }
}
