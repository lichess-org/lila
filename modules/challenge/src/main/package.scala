package lidraughts

import org.joda.time.DateTime

import lidraughts.socket.WithSocket

package object challenge extends PackageObject with WithSocket {

  type EitherChallenger = Either[Challenge.Anonymous, Challenge.Registered]

  private[challenge] def inTwoWeeks = DateTime.now plusWeeks 2
}
