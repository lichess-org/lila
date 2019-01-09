package lila

import org.joda.time.DateTime

import lila.socket.WithSocket

package object challenge extends PackageObject with WithSocket {

  type EitherChallenger = Either[Challenge.Anonymous, Challenge.Registered]

  private[challenge] type SocketMap = lila.hub.TrouperMap[ChallengeSocket]

  private[challenge] def inTwoWeeks = DateTime.now plusWeeks 2
}
