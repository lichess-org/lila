package lila.db

import scala.util.Success

import org.joda.time.DateTime
import reactivemongo.bson._

abstract class BSON[T]
    extends BSONHandler[BSONDocument, T]
    with BSONDocumentReader[T]
    with BSONDocumentWriter[T] {

  import BSON._

  def reads(reader: Reader): T
  def writes(writer: Writer, obj: T): BSONDocument

  def read(doc: BSONDocument): T = reads(new Reader(doc))
  def write(obj: T): BSONDocument = writes(writer, obj)
}

object BSON {

  implicit object BSONJodaDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(x: BSONDateTime) = new DateTime(x.value)
    def write(x: DateTime) = BSONDateTime(x.getMillis)
  }

  object Map {

    implicit def MapReader[V](implicit vr: BSONDocumentReader[V]): BSONDocumentReader[Map[String, V]] = new BSONDocumentReader[Map[String, V]] {
      def read(bson: BSONDocument): Map[String, V] =
        bson.elements.map { tuple =>
          // assume that all values in the document are BSONDocuments
          tuple._1 -> vr.read(tuple._2.seeAsTry[BSONDocument].get)
        }.toMap
    }

    implicit def MapWriter[V](implicit vw: BSONDocumentWriter[V]): BSONDocumentWriter[Map[String, V]] = new BSONDocumentWriter[Map[String, V]] {
      def write(map: Map[String, V]): BSONDocument = BSONDocument {
        map.toStream.map { tuple =>
          tuple._1 -> vw.write(tuple._2)
        }
      }
    }
  }

  object MapValue {

    implicit def MapReader[V](implicit vr: BSONReader[_ <: BSONValue, V]): BSONDocumentReader[Map[String, V]] = new BSONDocumentReader[Map[String, V]] {
      def read(bson: BSONDocument): Map[String, V] =
        bson.elements.map { tuple =>
          tuple._1 -> vr.asInstanceOf[BSONReader[BSONValue, V]].read(tuple._2)
        }.toMap
    }

    implicit def MapWriter[V](implicit vw: BSONWriter[V, _ <: BSONValue]): BSONDocumentWriter[Map[String, V]] = new BSONDocumentWriter[Map[String, V]] {
      def write(map: Map[String, V]): BSONDocument = BSONDocument {
        map.toStream.map { tuple =>
          tuple._1 -> vw.write(tuple._2)
        }
      }
    }

    implicit def MapHandler[V](implicit vr: BSONReader[_ <: BSONValue, V], vw: BSONWriter[V, _ <: BSONValue]): BSONHandler[BSONDocument, Map[String, V]] = new BSONHandler[BSONDocument, Map[String, V]] {
      private val reader = MapReader[V]
      private val writer = MapWriter[V]
      def read(bson: BSONDocument): Map[String, V] = reader read bson
      def write(map: Map[String, V]): BSONDocument = writer write map
    }
  }

  // List Handler
  final class ListHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]) extends BSONHandler[BSONArray, List[T]] {
    def read(array: BSONArray) = array.stream.filter(_.isSuccess).map { v =>
      reader.asInstanceOf[BSONReader[BSONValue, T]].read(v.get)
    }.toList
    def write(repr: List[T]) =
      new BSONArray(repr.map(s => scala.util.Try(writer.write(s))).to[Stream])
  }
  implicit def bsonArrayToListHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]): BSONHandler[BSONArray, List[T]] = new ListHandler

  final class Reader(val doc: BSONDocument) {

    val map = (doc.stream collect { case Success(e) => e }).toMap

    def get[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, A]): A =
      reader.asInstanceOf[BSONReader[BSONValue, A]] read map(k)
    def getO[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, A]): Option[A] =
      map get k flatMap reader.asInstanceOf[BSONReader[BSONValue, A]].readOpt
    def getD[A](k: String, default: A)(implicit reader: BSONReader[_ <: BSONValue, A]): A =
      getO[A](k) getOrElse default

    def str(k: String) = get[String](k)
    def strO(k: String) = getO[String](k)
    def strD(k: String) = strO(k) getOrElse ""
    def int(k: String) = get[Int](k)
    def intO(k: String) = getO[Int](k)
    def intD(k: String) = intO(k) getOrElse 0
    def double(k: String) = get[Double](k)
    def doubleO(k: String) = getO[Double](k)
    def doubleD(k: String) = doubleO(k) getOrElse 0
    def bool(k: String) = get[Boolean](k)
    def boolO(k: String) = getO[Boolean](k)
    def boolD(k: String) = boolO(k) getOrElse false
    def date(k: String) = get[DateTime](k)
    def dateO(k: String) = getO[DateTime](k)
    def bytes(k: String) = get[ByteArray](k)
    def bytesO(k: String) = getO[ByteArray](k)
    def bytesD(k: String) = bytesO(k) getOrElse ByteArray.empty
    def nInt(k: String) = get[BSONNumberLike](k).toInt
    def nIntO(k: String) = getO[BSONNumberLike](k) map (_.toInt)
    def nIntD(k: String) = nIntO(k) getOrElse 0
    def intsD(k: String) = getO[List[Int]](k) getOrElse Nil
    def strsD(k: String) = getO[List[String]](k) getOrElse Nil

    def toList = doc.elements.toList

    def debug = BSON debug doc
  }

  final class Writer {

    def boolO(b: Boolean): Option[BSONBoolean] = if (b) Some(BSONBoolean(true)) else None
    def str(s: String): BSONString = BSONString(s)
    def strO(s: String): Option[BSONString] = if (s.nonEmpty) Some(BSONString(s)) else None
    def int(i: Int): BSONInteger = BSONInteger(i)
    def intO(i: Int): Option[BSONInteger] = if (i != 0) Some(BSONInteger(i)) else None
    def date(d: DateTime): BSONDateTime = BSONJodaDateTimeHandler write d
    def byteArrayO(b: ByteArray): Option[BSONBinary] =
      if (b.isEmpty) None else ByteArray.ByteArrayBSONHandler.write(b).some
    def bytesO(b: Array[Byte]): Option[BSONBinary] = byteArrayO(ByteArray(b))
    def listO(list: List[String]): Option[List[String]] = list match {
      case Nil          => None
      case List("")     => None
      case List("", "") => None
      case List(a, "")  => Some(List(a))
      case full         => Some(full)
    }
    def docO(o: BSONDocument): Option[BSONDocument] = if (o.isEmpty) None else Some(o)
    def double(i: Double): BSONDouble = BSONDouble(i)
    def doubleO(i: Double): Option[BSONDouble] = if (i != 0) Some(BSONDouble(i)) else None
    def intsO(l: List[Int]): Option[BSONArray] =
      if (l.isEmpty) None
      else Some(BSONArray(l map BSONInteger.apply))

    import scalaz.Functor
    def map[M[_]: Functor, A, B <: BSONValue](a: M[A])(implicit writer: BSONWriter[A, B]): M[B] =
      a map writer.write
  }

  val writer = new Writer

  def debug(v: BSONValue): String = v match {
    case d: BSONDocument => debugDoc(d)
    case d: BSONArray    => debugArr(d)
    case v               => v.toString
  }
  def debugArr(doc: BSONArray): String = doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: BSONDocument): String = (doc.elements.toList map {
    case (k, v) => s"$k: ${debug(v)}"
  }).mkString("{", ", ", "}")
}
