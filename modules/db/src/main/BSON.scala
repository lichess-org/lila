package lila.db

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.api.bson._
import reactivemongo.api.bson.compat._
import reactivemongo.api.bson.exceptions.TypeDoesNotMatchException
import scala.util.{ Try, Success, Failure }

import dsl._
import lila.common.Iso

abstract class BSON[T]
  extends BSONReadOnly[T]
  with BSONHandler[T]
  with BSONDocumentReader[T]
  with BSONDocumentWriter[T] {

  import BSON._

  def writes(writer: Writer, obj: T): Bdoc

  def writeTry(obj: T) = Success(writes(writer, obj))

  def write(obj: T) = writes(writer, obj)
}

abstract class BSONReadOnly[T] extends BSONDocumentReader[T] {

  val logMalformed = true

  import BSON._

  def reads(reader: Reader): T

  def readDocument(doc: Bdoc) = Try {
    reads(new Reader(doc))
  }

  def read(doc: Bdoc) = readDocument(doc).get
}

object BSON extends Handlers {

  // def toDocHandler[A](implicit handler: BSONHandler[A]): BSONDocumentHandler[A] =
  //   new BSONDocumentReader[A] with BSONDocumentWriter[A] with BSONHandler[A] {
  //     def read(doc: BSONDocument) = handler read doc
  //     def write(o: A) = handler write o
  //   }

  // object MapDocument {

  //   implicit def MapReader[K, V](implicit kIso: Iso.StringIso[K], vr: BSONDocumentReader[V]): BSONDocumentReader[Map[K, V]] = new BSONDocumentReader[Map[K, V]] {
  //     def read(bson: Bdoc): Map[K, V] = {
  //       // mutable optimized implementation
  //       val b = Map.newBuilder[K, V]
  //       for (tuple <- bson.elements)
  //         // assume that all values in the document are Bdocs
  //         b += (kIso.from(tuple.name) -> vr.read(tuple.value.asInstanceOf[Bdoc]))
  //       b.result
  //     }
  //   }

  //   implicit def MapWriter[K, V](implicit kIso: Iso.StringIso[K], vw: reactivemongo.api.bson.BSONDocumentWriter[V]): BSONDocumentWriter[Map[K, V]] = new BSONDocumentWriter[Map[K, V]] {
  //     def write(map: Map[K, V]): Bdoc = BSONDocument {
  //       map.map { tuple =>
  //         kIso.to(tuple._1) -> vw.writeTry(tuple._2).get
  //       }
  //     }
  //   }

  //   implicit def MapHandler[K: Iso.StringIso, V: BSONDocumentHandler]: BSONHandler[Map[K, V]] = new BSONHandler[Map[K, V]] {
  //     private val reader = MapReader[K, V]
  //     private val writer = MapWriter[K, V]
  //     def read(bson: Bdoc): Map[K, V] = reader read bson
  //     def write(map: Map[K, V]): Bdoc = writer write map
  //   }
  // }

  // object MapValue {

  //   implicit def MapReader[K, V](implicit kIso: Iso.StringIso[K], vr: BSONReader[V]): BSONDocumentReader[Map[K, V]] = new BSONDocumentReader[Map[K, V]] {
  //     def read(bson: Bdoc): Map[K, V] = {
  //       val valueReader = vr.asInstanceOf[BSONReader[V]]
  //       // mutable optimized implementation
  //       val b = Map.newBuilder[K, V]
  //       for (tuple <- bson.elements) b += (kIso.from(tuple.name) -> valueReader.read(tuple.value))
  //       b.result
  //     }
  //   }

  //   implicit def MapWriter[K, V](implicit kIso: Iso.StringIso[K], vw: BSONWriter[V]): BSONDocumentWriter[Map[K, V]] = new BSONDocumentWriter[Map[K, V]] {
  //     def write(map: Map[K, V]): Bdoc = BSONDocument {
  //       map.toStream.map { tuple =>
  //         kIso.to(tuple._1) -> vw.writeTry(tuple._2).get
  //       }
  //     }
  //   }

  //   implicit def MapHandler[K, V](implicit kIso: Iso.StringIso[K], vr: BSONReader[V], vw: BSONWriter[V]): BSONHandler[Map[K, V]] = new BSONHandler[Map[K, V]] {
  //     private val reader = MapReader[K, V]
  //     private val writer = MapWriter[K, V]
  //     def read(bson: Bdoc): Map[K, V] = reader read bson
  //     def write(map: Map[K, V]): Bdoc = writer write map
  //   }
  // }

  def quickHandler[T](read: PartialFunction[BSONValue, T], write: T => BSONValue): BSONHandler[T] = new BSONHandler[T] {
    def readTry(bson: BSONValue) = read.andThen(Success(_)).applyOrElse(
      bson,
      (b: BSONValue) => handlerBadType(b)
    )
    def writeTry(t: T) = Success(write(t))
  }

  def tryHandler[T](read: PartialFunction[BSONValue, Try[T]], write: T => BSONValue): BSONHandler[T] = new BSONHandler[T] {
    def readTry(bson: BSONValue) = read.applyOrElse(
      bson,
      (b: BSONValue) => handlerBadType(b)
    )
    def writeTry(t: T) = Success(write(t))
  }

  def handlerBadType[T](b: BSONValue): Try[T] =
    Failure(TypeDoesNotMatchException("BSONBinary", b.getClass.getSimpleName))

  def handlerBadValue[T](msg: String): Try[T] =
    Failure(new IllegalArgumentException(msg))

  final class Reader(val doc: Bdoc) {

    val map = {
      // mutable optimized implementation
      val b = Map.newBuilder[String, BSONValue]
      for (tuple <- doc.stream if tuple.isSuccess) b += (tuple.get.name -> tuple.get.value)
      b.result
    }

    def get[A](k: String)(implicit reader: BSONReader[A]): A =
      reader.readTry(map(k)).get
    // reader.asInstanceOf[BSONReader[A]].readTry(map(k)).get
    def getO[A](k: String)(implicit reader: BSONReader[A]): Option[A] =
      map get k flatMap reader.readOpt
    def getD[A](k: String)(implicit zero: Zero[A], reader: BSONReader[A]): A =
      getO[A](k) getOrElse zero.zero
    def getD[A](k: String, default: => A)(implicit reader: BSONReader[A]): A =
      getO[A](k) getOrElse default
    def getsD[A](k: String)(implicit reader: BSONReader[List[A]]) =
      getO[List[A]](k) getOrElse Nil

    def str(k: String) = get[String](k)(BSONStringHandler)
    def strO(k: String) = getO[String](k)(BSONStringHandler)
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
    def nInt(k: String) = get[BSONNumberLike](k).toInt.get
    def nIntO(k: String): Option[Int] = getO[BSONNumberLike](k) flatMap (_.toInt.toOption)
    def nIntD(k: String): Int = nIntO(k) getOrElse 0
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
    def date(d: DateTime): BSONValue = BSONJodaDateTimeHandler.writeTry(d).get
    def byteArrayO(b: ByteArray): Option[BSONValue] =
      if (b.isEmpty) None else ByteArray.ByteArrayBSONHandler.writeOpt(b)
    def bytesO(b: Array[Byte]): Option[BSONValue] = byteArrayO(ByteArray(b))
    def bytes(b: Array[Byte]): BSONBinary = BSONBinary(b, ByteArray.subtype)
    def strListO(list: List[String]): Option[List[String]] = list match {
      case Nil => None
      case List("") => None
      case List("", "") => None
      case List(a, "") => Some(List(a))
      case full => Some(full)
    }
    def listO[A](list: List[A])(implicit writer: BSONWriter[A]): Option[Barr] =
      if (list.isEmpty) None
      else Some(BSONArray(list flatMap writer.writeOpt))
    def docO(o: Bdoc): Option[Bdoc] = if (o.isEmpty) None else Some(o)
    def double(i: Double): BSONDouble = BSONDouble(i)
    def doubleO(i: Double): Option[BSONDouble] = if (i != 0) Some(BSONDouble(i)) else None
    def zero[A](a: A)(implicit zero: Zero[A]): Option[A] = if (zero.zero == a) None else Some(a)

    // import scalaz.Functor
    // def map[M[_]: Functor, A, B <: BSONValue](a: M[A])(implicit writer: BSONWriter[A]): M[B] =
    //   a map writer.write
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
