package lila.relay

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

private[relay] final class Telnet(remote: InetSocketAddress, listener: ActorRef) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote, options = List(
    SO.TcpNoDelay(false)
    // next lines seem to have no effect at all, messages are still truncated
    // SO.ReceiveBufferSize(1024 * 1024),
    // SO.SendBufferSize(1024 * 1024)
  ))

  def receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self

    case Connected(remote, local) =>
      val connection = sender()
      connection ! Register(self)
      listener ! Telnet.Connection({ str =>
        println(s"FICS> $str")
        connection ! Write(ByteString(s"$str\n"))
      })
      context become {
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! Telnet.WriteFailed
        case Received(data) =>
          val msg = data decodeString "UTF-8"
          println(s"<telnet>$msg</telnet>")
          listener ! Telnet.In(msg)
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! Telnet.Close
          context stop self
      }
  }
}

object Telnet {

  case class In(data: String)
  case class Connection(send: String => Unit)
  case object ConnectFailed
  case object WriteFailed
  case object Close
}
