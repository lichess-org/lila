package controllers

import lila.api._

import play.api.http._
import play.api.mvc.Codec

trait ResponseWriter {

  implicit def wUnit(implicit codec: Codec): Writeable[Unit] =
    Writeable[Unit]((_: Unit) => codec encode "ok")
  implicit def ctoUnit: ContentTypeOf[Unit] =
    ContentTypeOf[Unit](Some(ContentTypes.TEXT))

  implicit def wLong(implicit codec: Codec): Writeable[Long] =
    Writeable[Long]((a: Long) => codec encode a.toString)
  implicit def ctoLong: ContentTypeOf[Long] =
    ContentTypeOf[Long](Some(ContentTypes.TEXT))

  implicit def wInt(implicit codec: Codec): Writeable[Int] =
    Writeable[Int]((i: Int) => codec encode i.toString)
  implicit def ctoInt: ContentTypeOf[Int] =
    ContentTypeOf[Int](Some(ContentTypes.TEXT))

  implicit def wOptionString(implicit codec: Codec): Writeable[Option[String]] =
    Writeable[Option[String]]((i: Option[String]) => codec encode ~i)
  implicit def ctoOptionString: ContentTypeOf[Option[String]] =
    ContentTypeOf[Option[String]](Some(ContentTypes.TEXT))
}
