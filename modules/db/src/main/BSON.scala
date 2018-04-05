package lila.db

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.bson._

import dsl._
import lila.common.Iso

abstract class BSON[T]
  extends BSONReadOnly[T]
  with BSONHandler[Bdoc, T]
  with BSONDocumentReader[T]
  with BSONDocumentWriter[T] {

  import BSON._

  def writes(writer: Writer, obj: T): Bdoc

  def write(obj: T): Bdoc = writes(writer, obj)
}

abstract class BSONReadOnly[T] extends BSONDocumentReader[T] {

  val logMalformed = true

  import BSON._

  def reads(reader: Reader): T

  def read(doc: Bdoc): T = if (logMalformed) try {
    reads(new Reader(doc))
  } catch {
    case e: Exception =>
      logger.warn(s"Can't read malformed doc ${debug(doc)}", e)
      throw e
  }
  else reads(new Reader(doc))
}

object BSON extends Handlers {

  def toDocHandler[A](implicit handler: BSONHandler[BSONDocument, A]): BSONDocumentHandler[A] =
    new BSONDocumentReader[A] with BSONDocumentWriter[A] with BSONHandler[BSONDocument, A] {
      def read(doc: BSONDocument) = handler read doc
      def write(o: A) = handler write o
    }

  def LoggingHandler[T](logger: lila.log.Logger)(handler: BSONHandler[Bdoc, T]): BSONHandler[Bdoc, T] with BSONDocumentReader[T] with BSONDocumentWriter[T] =
    new BSONHandler[Bdoc, T] with BSONDocumentReader[T] with BSONDocumentWriter[T] {
      def read(doc: Bdoc): T = try {
        handler read doc
      } catch {
        case e: Exception =>
          logger.warn(s"Can't read malformed doc ${debug(doc)}", e)
          throw e
      }
      def write(obj: T): Bdoc = handler write obj
    }

  object MapDocument {

    implicit def MapReader[K, V](implicit kIso: Iso.StringIso[K], vr: BSONDocumentReader[V]): BSONDocumentReader[Map[K, V]] = new BSONDocumentReader[Map[K, V]] {
      def read(bson: Bdoc): Map[K, V] = {
        // mutable optimized implementation
        val b = collection.immutable.Map.newBuilder[K, V]
        for (tuple <- bson.elements)
          // assume that all values in the document are Bdocs
          b += (kIso.from(tuple.name) -> vr.read(tuple.value.asInstanceOf[Bdoc]))
        b.result
      }
    }

    implicit def MapWriter[K, V](implicit kIso: Iso.StringIso[K], vw: BSONDocumentWriter[V]): BSONDocumentWriter[Map[K, V]] = new BSONDocumentWriter[Map[K, V]] {
      def write(map: Map[K, V]): Bdoc = BSONDocument {
        map.toStream.map { tuple =>
          kIso.to(tuple._1) -> vw.write(tuple._2)
        }
      }
    }

    implicit def MapHandler[K: Iso.StringIso, V: BSONDocumentHandler]: BSONHandler[Bdoc, Map[K, V]] = new BSONHandler[Bdoc, Map[K, V]] {
      private val reader = MapReader[K, V]
      private val writer = MapWriter[K, V]
      def read(bson: Bdoc): Map[K, V] = reader read bson
      def write(map: Map[K, V]): Bdoc = writer write map
    }
  }

  object MapValue {

    implicit def MapReader[K, V](implicit kIso: Iso.StringIso[K], vr: BSONReader[_ <: BSONValue, V]): BSONDocumentReader[Map[K, V]] = new BSONDocumentReader[Map[K, V]] {
      def read(bson: Bdoc): Map[K, V] = {
        val valueReader = vr.asInstanceOf[BSONReader[BSONValue, V]]
        // mutable optimized implementation
        val b = collection.immutable.Map.newBuilder[K, V]
        for (tuple <- bson.elements) b += (kIso.from(tuple.name) -> valueReader.read(tuple.value))
        b.result
      }
    }

    implicit def MapWriter[K, V](implicit kIso: Iso.StringIso[K], vw: BSONWriter[V, _ <: BSONValue]): BSONDocumentWriter[Map[K, V]] = new BSONDocumentWriter[Map[K, V]] {
      def write(map: Map[K, V]): Bdoc = BSONDocument {
        map.toStream.map { tuple =>
          kIso.to(tuple._1) -> vw.write(tuple._2)
        }
      }
    }

    implicit def MapHandler[K, V](implicit kIso: Iso.StringIso[K], vr: BSONReader[_ <: BSONValue, V], vw: BSONWriter[V, _ <: BSONValue]): BSONHandler[Bdoc, Map[K, V]] = new BSONHandler[Bdoc, Map[K, V]] {
      private val reader = MapReader[K, V]
      private val writer = MapWriter[K, V]
      def read(bson: Bdoc): Map[K, V] = reader read bson
      def write(map: Map[K, V]): Bdoc = writer write map
    }
  }

  final class Reader(val doc: BSONDocument) {

    val map = {
      // mutable optimized implementation
      val b = collection.immutable.Map.newBuilder[String, BSONValue]
      for (tuple <- doc.stream if tuple.isSuccess) b += (tuple.get.name -> tuple.get.value)
      b.result
    }

    def get[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, A]): A =
      reader.asInstanceOf[BSONReader[BSONValue, A]] read map(k)
    def getO[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, A]): Option[A] =
      map get k flatMap reader.asInstanceOf[BSONReader[BSONValue, A]].readOpt
    def getD[A](k: String)(implicit zero: Zero[A], reader: BSONReader[_ <: BSONValue, A]): A =
      getO[A](k) getOrElse zero.zero
    def getD[A](k: String, default: => A)(implicit reader: BSONReader[_ <: BSONValue, A]): A =
      getO[A](k) getOrElse default
    def getsD[A](k: String)(implicit reader: BSONReader[_ <: BSONValue, List[A]]) =
      getO[List[A]](k) getOrElse Nil

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
    def dateD(k: String, default: => DateTime) = getD(k, default)
    def bytes(k: String) = get[ByteArray](k)
    def bytesO(k: String) = getO[ByteArray](k)
    def bytesD(k: String) = bytesO(k) getOrElse ByteArray.empty
    def nInt(k: String) = get[BSONNumberLike](k).toInt
    def nIntO(k: String) = getO[BSONNumberLike](k) map (_.toInt)
    def nIntD(k: String) = nIntO(k) getOrElse 0
    def intsD(k: String) = getO[List[Int]](k) getOrElse Nil
    def strsD(k: String) = getO[List[String]](k) getOrElse Nil

    def toList = doc.elements.toList

    def contains = map contains _

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
    def bytes(b: Array[Byte]): BSONBinary = BSONBinary(b, ByteArray.subtype)
    def strListO(list: List[String]): Option[List[String]] = list match {
      case Nil => None
      case List("") => None
      case List("", "") => None
      case List(a, "") => Some(List(a))
      case full => Some(full)
    }
    def listO[A](list: List[A])(implicit writer: BSONWriter[A, _ <: BSONValue]): Option[Barr] =
      if (list.isEmpty) None
      else Some(BSONArray(list map writer.write))
    def docO(o: Bdoc): Option[Bdoc] = if (o.isEmpty) None else Some(o)
    def double(i: Double): BSONDouble = BSONDouble(i)
    def doubleO(i: Double): Option[BSONDouble] = if (i != 0) Some(BSONDouble(i)) else None
    def zero[A](a: A)(implicit zero: Zero[A]): Option[A] = if (zero.zero == a) None else Some(a)

    import scalaz.Functor
    def map[M[_]: Functor, A, B <: BSONValue](a: M[A])(implicit writer: BSONWriter[A, B]): M[B] =
      a map writer.write
  }

  val writer = new Writer

  def debug(v: BSONValue): String = v match {
    case d: Bdoc => debugDoc(d)
    case d: Barr => debugArr(d)
    case BSONString(x) => x
    case BSONInteger(x) => x.toString
    case BSONDouble(x) => x.toString
    case BSONBoolean(x) => x.toString
    case v => v.toString
  }
  def debugArr(doc: Barr): String = doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: Bdoc): String = (doc.elements.toList map {
    case BSONElement(k, v) => s"$k: ${debug(v)}"
  }).mkString("{", ", ", "}")

  def hashDoc(doc: Bdoc): String = debugDoc(doc).replace(" ", "")
}
