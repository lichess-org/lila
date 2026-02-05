package lila.core.lilaism

import scala.util.control.NoStackTrace

trait LilaException extends Exception:
  val message: String

  override def getMessage = message
  override def toString = message

trait LilaExceptionNoStack extends LilaException with NoStackTrace

case class LilaInvalid(message: String) extends LilaException
case class LilaNoStackTrace(message: String) extends LilaExceptionNoStack

object LilaException:

  def apply(msg: String) =
    new LilaException:
      val message = msg
