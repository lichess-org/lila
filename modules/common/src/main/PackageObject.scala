package lila

import scala.concurrent.Future

import ornicar.scalalib
import scalaz.{ Monad, Monoid, OptionT }

trait PackageObject extends Steroids with WithFuture {

  def !![A](msg: String): Valid[A] = msg.failNel[A]

  def nowMillis: Long = System.currentTimeMillis
  def nowTenths: Long = nowMillis / 100
  def nowSeconds: Int = (nowMillis / 1000).toInt

  lazy val logger = play.api.Logger("lila")
  def loginfo(s: String) { logger info s }
  def logwarn(s: String) { logger warn s }
  def logerr(s: String) { logger error s }
  def fuloginfo(s: String) = fuccess { loginfo(s) }
  def fulogwarn(s: String) = fuccess { logwarn(s) }
  def fulogerr(s: String) = fuccess { logerr(s) }

  implicit final def runOptionT[F[+_], A](ot: OptionT[F, A]): F[Option[A]] = ot.run

  implicit final class LilaPimpedString(s: String) {

    def describes[A](v: => A): A = { loginfo(s); v }
  }

  implicit final class LilaPimpedValid[A](v: Valid[A]) {

    def future: Fu[A] = v fold (errs => fufail(errs.shows), fuccess)
  }

  implicit final class LilaPimpedTry[A](v: scala.util.Try[A]) {

    def fold[B](fe: Exception => B, fa: A => B): B = v match {
      case scala.util.Failure(e: Exception) => fe(e)
      case scala.util.Failure(e)            => throw e
      case scala.util.Success(a)            => fa(a)
    }

    def future: Fu[A] = fold(Future.failed, fuccess)
  }

  def parseIntOption(str: String): Option[Int] = try {
    Some(java.lang.Integer.parseInt(str))
  }
  catch {
    case e: NumberFormatException => None
  }

  def parseFloatOption(str: String): Option[Float] = try {
    Some(java.lang.Float.parseFloat(str))
  }
  catch {
    case e: NumberFormatException => None
  }

  def intBox(in: Range.Inclusive)(v: Int): Int =
    math.max(in.start, math.min(v, in.end))

  def floatBox(in: Range.Inclusive)(v: Float): Float =
    math.max(in.start, math.min(v, in.end))

  def doubleBox(in: Range.Inclusive)(v: Double): Double =
    math.max(in.start, math.min(v, in.end))
}

trait WithFuture extends scalalib.Validation {

  type Fu[+A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Exception, B](a: A): Fu[B] = Future failed a
  def fufail[A](a: String): Fu[A] = fufail(common.LilaException(a))
  def fufail[A](a: Failures): Fu[A] = fufail(common.LilaException(a))
  val funit = fuccess(())

  implicit def SprayPimpedFuture[T](fut: Future[T]) =
    new spray.util.pimps.PimpedFuture[T](fut)
}

trait WithPlay { self: PackageObject =>

  import play.api.libs.json._
  import scalalib.Zero

  implicit def execontext = play.api.libs.concurrent.Execution.defaultContext

  implicit val LilaFutureInstances = new Monad[Fu] {
    override def map[A, B](fa: Fu[A])(f: A => B) = fa map f
    def point[A](a: => A) = fuccess(a)
    def bind[A, B](fa: Fu[A])(f: A => Fu[B]) = fa flatMap f
  }

  implicit def LilaFuMonoid[A: Monoid]: Monoid[Fu[A]] =
    Monoid.instance((x, y) => x zip y map {
      case (a, b) => a ⊹ b
    }, fuccess(∅[A]))

  implicit def LilaFuZero[A: Zero]: Zero[Fu[A]] =
    Zero.instance(fuccess(zero[A]))

  implicit val LilaJsObjectZero: Zero[JsObject] =
    Zero.instance(JsObject(Seq.empty))

  implicit def LilaJsResultZero[A]: Zero[JsResult[A]] =
    Zero.instance(JsError(Seq.empty))

  implicit final class LilaTraversableFuture[A, M[_] <: TraversableOnce[_]](t: M[Fu[A]]) {

    def sequenceFu(implicit cbf: scala.collection.generic.CanBuildFrom[M[Fu[A]], A, M[A]]) = Future sequence t
  }

  implicit final class LilaPimpedFuture[A](fua: Fu[A]) {

    def >>-(sideEffect: => Unit): Fu[A] = fua andThen {
      case _ => sideEffect
    }

    def >>[B](fub: => Fu[B]): Fu[B] = fua flatMap (_ => fub)

    def void: Funit = fua map (_ => Unit)

    def inject[B](b: => B): Fu[B] = fua map (_ => b)

    def effectFold(fail: Exception => Unit, succ: A => Unit) {
      fua onComplete {
        case scala.util.Failure(e: Exception) => fail(e)
        case scala.util.Failure(e)            => throw e // Throwables
        case scala.util.Success(e)            => succ(e)
      }
    }

    def fold[B](fail: Exception => B, succ: A => B): Fu[B] =
      fua map succ recover { case e: Exception => fail(e) }

    def flatFold[B](fail: Exception => Fu[B], succ: A => Fu[B]): Fu[B] =
      fua flatMap succ recoverWith { case e: Exception => fail(e) }

    def logFailure(prefix: Throwable => String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception => logwarn(prefix(e) + " " + e.toString)
    })
    def logFailure(prefix: => String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception => logwarn(prefix + " " + e.toString)
    })
    def logFailureErr(prefix: => String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception => logerr(prefix + " " + e.toString)
    })

    def addEffect(effect: A => Unit) = fua ~ (_ foreach effect)

    def addFailureEffect(effect: Exception => Unit) = fua ~ (_ onFailure {
      case e: Exception => effect(e)
    })

    def thenPp: Fu[A] = fua ~ {
      _.effectFold(
        e => println("[failure] " + e),
        a => println("[success] " + a)
      )
    }

    def thenPp(msg: String): Fu[A] = fua ~ {
      _.effectFold(
        e => println(s"[$msg] [failure] $e"),
        a => println(s"[$msg] [success] $a")
      )
    }
  }

  implicit final class LilaPimpedFutureZero[A: Zero](fua: Fu[A]) {

    def nevermind(msg: String): Fu[A] = fua recover {
      case e: lila.common.LilaException             => recoverException(e, msg.some)
      case e: java.util.concurrent.TimeoutException => recoverException(e, msg.some)
    }

    def nevermind: Fu[A] = nevermind("")

    private def recoverException(e: Exception, msg: Option[String]) = {
      logwarn(msg.filter(_.nonEmpty).??(_ + ": ") + e.getMessage)
      zero[A]
    }
  }

  implicit final class LilaPimpedFutureOption[A](fua: Fu[Option[A]]) {

    def flatten(msg: => String): Fu[A] = fua flatMap {
      _.fold[Fu[A]](fufail(msg))(fuccess(_))
    }

    def orElse(other: => Fu[Option[A]]): Fu[Option[A]] = fua flatMap {
      _.fold(other) { x => fuccess(x.some) }
    }

    def getOrElse(other: => Fu[A]): Fu[A] = fua flatMap { _.fold(other)(fuccess) }
  }

  implicit final class LilaPimpedFutureValid[A](fua: Fu[Valid[A]]) {

    def flatten: Fu[A] = fua flatMap { _.fold[Fu[A]](fufail(_), fuccess(_)) }
  }

  implicit final class LilaPimpedFutureBoolean(fua: Fu[Boolean]) {

    def >>&(fub: => Fu[Boolean]): Fu[Boolean] =
      fua flatMap { _.fold(fub, fuccess(false)) }

    def >>|(fub: => Fu[Boolean]): Fu[Boolean] =
      fua flatMap { _.fold(fuccess(true), fub) }

    def unary_! = fua map (!_)
  }

  implicit final class LilaPimpedBooleanWithFuture(self: Boolean) {

    def optionFu[A](v: => Fu[A]): Fu[Option[A]] = if (self) v map (_.some) else fuccess(none)
  }

  implicit final class LilaPimpedActorSystem(self: akka.actor.ActorSystem) {

    def lilaBus = lila.common.Bus(self)
  }

  object makeTimeout {

    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val short = seconds(1)
    implicit val large = seconds(5)
    implicit val veryLarge = minutes(5)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def seconds(s: Int): Timeout = Timeout(s.seconds)
    def minutes(m: Int): Timeout = Timeout(m.minutes)
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit): Funit = Future {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter => Unit): Funit =
    printToFile(new java.io.File(f))(op)
}
