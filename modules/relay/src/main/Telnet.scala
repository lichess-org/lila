package lila.relay

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

private[relay] final class Telnet(
    remote: InetSocketAddress,
    listener: ActorRef) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote, options = List(
    SO.TcpNoDelay(false)
  ))

  var bufferUntil = none[String]
  val buffer = new collection.mutable.StringBuilder

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
          val chunk = data decodeString "UTF-8"
          bufferUntil match {
            case None => listener ! Telnet.In(chunk)
            case Some(eom) =>
              buffer append chunk
              if (buffer endsWith eom) {
                listener ! Telnet.In(buffer.toString)
                buffer.clear()
              }
          }
        case Telnet.BufferUntil(str) =>
          buffer.clear()
          bufferUntil = str
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! Telnet.Close
          context stop self
      }
  }
}

object Telnet {

  case class In(data: String) {
    def lines: List[String] = data.split(Array('\r', '\n')).toList.filter(_.nonEmpty).map(_.trim)
    def last: Option[String] = lines.lastOption
  }
  case class Connection(send: String => Unit)
  case class BufferUntil(str: Option[String])
  case object ConnectFailed
  case object WriteFailed
  case object Close
}
