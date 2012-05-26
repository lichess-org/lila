package controllers

import lila._

import play.api.http._
import scala.io.Codec

trait ResponseWriter {

  // I like Unit requests.
  implicit def wUnit: Writeable[Unit] =
    Writeable[Unit](_ ⇒ Codec toUTF8 "ok")
  implicit def ctoUnit: ContentTypeOf[Unit] =
    ContentTypeOf[Unit](Some(ContentTypes.TEXT))

  implicit def wFloat: Writeable[Float] =
    Writeable[Float](f ⇒ Codec toUTF8 f.toString)
  implicit def ctoFloat: ContentTypeOf[Float] =
    ContentTypeOf[Float](Some(ContentTypes.TEXT))

  implicit def wLong: Writeable[Long] =
    Writeable[Long](a ⇒ Codec toUTF8 a.toString)
  implicit def ctoLong: ContentTypeOf[Long] =
    ContentTypeOf[Long](Some(ContentTypes.TEXT))

  implicit def wInt: Writeable[Int] =
    Writeable[Int](i ⇒ Codec toUTF8 i.toString)
  implicit def ctoInt: ContentTypeOf[Int] =
    ContentTypeOf[Int](Some(ContentTypes.TEXT))

  implicit def wOptionString: Writeable[Option[String]] =
    Writeable[Option[String]](i ⇒ Codec toUTF8 i.getOrElse(""))
  implicit def ctoOptionString: ContentTypeOf[Option[String]] =
    ContentTypeOf[Option[String]](Some(ContentTypes.TEXT))
}
