package lila

import org.joda.time.DateTime

import lila.socket.WithSocket

package object challenge extends PackageObject with WithPlay with WithSocket {

  type EitherChallenger = Either[Challenge.Anonymous, Challenge.Registered]

  private[challenge] def inTwoWeeks = DateTime.now plusWeeks 2
}
