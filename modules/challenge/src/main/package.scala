package lila

import lila.socket.WithSocket

package object challenge extends PackageObject with WithPlay with WithSocket {

  type EitherChallenger = Either[Challenge.Anonymous, Challenge.Registered]
}
