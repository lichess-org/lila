package lila.core.lilaism

import scala.util.control.NoStackTrace

trait LilaException extends Exception:
  val message: String

  override def getMessage = message
  override def toString   = message

case class LilaInvalid(message: String)      extends LilaException
case class LilaNoStackTrace(message: String) extends LilaException with NoStackTrace

object LilaException:

  def apply(msg: String) =
    new LilaException:
      val message = msg
