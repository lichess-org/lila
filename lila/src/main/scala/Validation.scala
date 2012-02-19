package ornicar

import util.control.Exception.allCatch
import scalaz.{ Validation ⇒ SValidation, Success, Failure, Semigroup, Apply, NonEmptyList }

trait Validation
    extends scalaz.Validations
    with scalaz.Semigroups
    with scalaz.Options
    with scalaz.MABs
    with scalaz.Identitys {

  case class Error(messages: NonEmptyList[String]) {

    def |+|(error: Error): Error = Error(messages |+| error.messages)

    def size = messages.list.size

    override def toString = messages.list map ("* " + _) mkString "\n"
  }

  object Error {

    def apply(t: Throwable): Error = string(t.getMessage)

    def string(str: String): Error = Error(str wrapNel)
  }

  type Valid[A] = SValidation[Error, A]

  implicit def eitherToValidation[E, B](either: Either[E, B]): Valid[B] =
    validation(either.left map {
      case e: Error => e
      case e: Throwable => Error(e)
      case s: String => Error(s wrapNel)
    })

  implicit def stringToError(str: String): Error = Error(str wrapNel)

  implicit def richStringToError(str: String) = new {
    def toError: Error = stringToError(str)
  }

  implicit def errorSemigroup: Semigroup[Error] = semigroup(_ |+| _)

  implicit def richValidation[A](validation: Valid[A]) = new {
    def and[B](f: Valid[A => B])(implicit a: Apply[Valid]): Valid[B] = validation <*> f
  }

  def unsafe[A](op: ⇒ A)(implicit handle: Throwable => Error = Error.apply): Valid[A] =
    (allCatch either op).left map handle

  def validateOption[A, B](ao: Option[A])(op: A => Valid[B]): Valid[Option[B]] =
    ao some { a ⇒ op(a) map (_.some) } none success(none)

  def sequenceValid[A](as: List[Valid[A]]): Valid[List[A]] =
    as.sequence[({ type λ[α] = Valid[α] })#λ, A]
}
