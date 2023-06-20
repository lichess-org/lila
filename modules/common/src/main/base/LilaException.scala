package lila.base

import scala.util.control.NoStackTrace

trait LilaException extends Exception:
  val message: String

  override def getMessage = message
  override def toString   = message

case class LilaInvalid(message: String)      extends LilaException
case class LilaTimeout(message: String)      extends LilaException with NoStackTrace
case class LilaNoStackTrace(message: String) extends LilaException with NoStackTrace

object LilaException:

  def apply(msg: String) =
    new LilaException:
      val message = msg
