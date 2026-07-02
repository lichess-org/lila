package lila.core.lilaism

trait LilaException extends Exception:
  val message: String

  override def getMessage = message
  override def toString = message

// similar to scala.util.control.NoStackTrace
trait LilaExceptionNoStack extends LilaException:
  override def fillInStackTrace(): Throwable = this

case class LilaInvalid(message: String) extends LilaException
case class LilaNoStackTrace(message: String) extends LilaExceptionNoStack

object LilaException:

  def apply(msg: String) =
    new LilaException:
      val message = msg
