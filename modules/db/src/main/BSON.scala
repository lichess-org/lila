package lila.db

import alleycats.Zero
import reactivemongo.api.bson.*

import scala.util.{ Success, Try }

import dsl.*

abstract class BSON[T]
    extends BSONReadOnly[T]
    with BSONDocumentReader[T]
    with BSONDocumentWriter[T]
    with BSONDocumentHandler[T]:

  import BSON.*

  def writes(writer: Writer, obj: T): Bdoc

  def writeTry(obj: T) = Success(writes(writer, obj))

  def write(obj: T) = writes(writer, obj)

abstract class BSONReadOnly[T] extends BSONDocumentReader[T]:

  import BSON.*

  def reads(reader: Reader): T

  def readDocument(doc: Bdoc) = Try:
    reads(new Reader(doc))

  def read(doc: Bdoc) = readDocument(doc).get

object BSON extends Handlers:

  final class Reader(val doc: Bdoc):

    inline def get[A: BSONReader](k: String): A =
      doc.getAsTry[A](k).get
    inline def getO[A: BSONReader](k: String): Option[A] =
      doc.getAsOpt[A](k)
    inline def getD[A](k: String)(using zero: Zero[A], reader: BSONReader[A]): A =
      doc.getAsOpt[A](k).getOrElse(zero.zero)
    inline def getD[A: BSONReader](k: String, default: => A): A =
      doc.getAsOpt[A](k).getOrElse(default)
    inline def getsD[A: BSONReader](k: String) =
      doc.getAsOpt[List[A]](k).getOrElse(Nil)

    inline def str(k: String)                        = get[String](k)(using BSONStringHandler)
    inline def strO(k: String)                       = getO[String](k)(using BSONStringHandler)
    inline def strD(k: String)                       = strO(k).getOrElse("")
    inline def int(k: String)                        = get[Int](k)
    inline def intO(k: String)                       = getO[Int](k)
    inline def intD(k: String)                       = intO(k).getOrElse(0)
    inline def double(k: String)                     = get[Double](k)
    inline def doubleO(k: String)                    = getO[Double](k)
    inline def floatO(k: String)                     = getO[Float](k)
    inline def bool(k: String)                       = get[Boolean](k)
    inline def boolO(k: String)                      = getO[Boolean](k)
    inline def boolD(k: String)                      = boolO(k).getOrElse(false)
    inline def date(k: String)                       = get[Instant](k)
    inline def dateO(k: String)                      = getO[Instant](k)
    inline def dateD(k: String, default: => Instant) = getD(k, default)
    inline def bytes(k: String)                      = get[ByteArray](k)
    inline def bytesO(k: String)                     = getO[ByteArray](k)
    inline def bytesD(k: String)                     = bytesO(k).getOrElse(ByteArray.empty)
    inline def nInt(k: String)                       = get[BSONNumberLike](k).toInt.get
    inline def nIntO(k: String): Option[Int]         = getO[BSONNumberLike](k).flatMap(_.toInt.toOption)
    inline def nIntD(k: String): Int                 = nIntO(k).getOrElse(0)
    inline def intsD(k: String)                      = getO[List[Int]](k).getOrElse(Nil)
    inline def strsD(k: String)                      = getO[List[String]](k).getOrElse(Nil)
    inline def yesnoD[A](k: String)(using sr: SameRuntime[A, Boolean], rs: SameRuntime[Boolean, A]): A =
      getO[A](k).getOrElse(rs(false))

    inline def contains = doc.contains

    inline def as[A](using r: BSONReader[A]) = r.readTry(doc).get

    def debug = BSON.debug(doc)

  final class Writer:

    def apply[A](a: A)(using writer: BSONWriter[A]): BSONValue = writer.writeTry(a).get
    def boolO(b: Boolean): Option[BSONBoolean]                 = if b then Some(BSONBoolean(true)) else None
    def str(s: String): BSONString                             = BSONString(s)
    def strO(s: String): Option[BSONString] = if s.nonEmpty then Some(BSONString(s)) else None
    def int(i: Int): BSONInteger            = BSONInteger(i)
    def intO(i: Int): Option[BSONInteger]   = if i != 0 then Some(BSONInteger(i)) else None
    def date(d: Instant)(using handler: BSONHandler[Instant]): BSONValue = handler.writeTry(d).get
    def byteArrayO(b: ByteArray)(using handler: BSONHandler[ByteArray]): Option[BSONValue] =
      if b.isEmpty then None else handler.writeOpt(b)
    def bytesO(b: Array[Byte]): Option[BSONValue] = byteArrayO(ByteArray(b))
    def bytes(b: Array[Byte]): BSONBinary         = BSONBinary(b, ByteArray.subtype)
    def listO[A](list: List[A])(using writer: BSONWriter[A]): Option[Barr] =
      if list.isEmpty then None
      else Some(BSONArray(list.flatMap(writer.writeOpt)))
    def docO(o: Bdoc): Option[Bdoc]                   = if o.isEmpty then None else Some(o)
    def double(i: Double): BSONDouble                 = BSONDouble(i)
    def doubleO(i: Double): Option[BSONDouble]        = if i != 0 then Some(BSONDouble(i)) else None
    def zero[A](a: A)(using zero: Zero[A]): Option[A] = if zero.zero == a then None else Some(a)
    def yesnoO[A](a: A)(using sr: SameRuntime[A, Boolean]): Option[BSONBoolean] =
      boolO(sr(a))

  val writer = new Writer

  def debug(v: BSONValue): String =
    v match
      case d: Bdoc        => debugDoc(d)
      case d: Barr        => debugArr(d)
      case BSONString(x)  => x
      case BSONInteger(x) => x.toString
      case BSONDouble(x)  => x.toString
      case BSONBoolean(x) => x.toString
      case v              => v.toString
  def debugArr(doc: Barr): String = doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: Bdoc): String =
    (doc.elements.toList
      .map {
        case BSONElement(k, v) => s"$k: ${debug(v)}"
        case x                 => x.toString
      })
      .mkString("{", ", ", "}")

  def print(v: BSONValue): Unit = println(debug(v))

  def hashDoc(doc: Bdoc): String = debugDoc(doc).replace(" ", "")
