package shogi
package format.usi

import shogi.variant.Variant

object Binary {

  def decodeMoves(bs: Seq[Byte], variant: Variant): Vector[Usi] = 
    Reader.decode(bs, variant)
  def decodeMoves(bs: Seq[Byte], variant: Variant,  nb: Int): Vector[Usi] = 
    Reader.decode(bs, variant, nb)

  def encodeMoves(ms: Seq[Usi], variant: Variant): Array[Byte] = 
    Writer.encode(ms, variant)

  private object Encoding {
    val roleToInt: Map[Role, Int] = Map(
      King           -> 0,
      Pawn           -> 1,
      Lance          -> 2,
      Knight         -> 3,
      Silver         -> 4,
      Gold           -> 5,
      Bishop         -> 6,
      Rook           -> 7,
      Tokin          -> 8,
      PromotedLance  -> 9,
      PromotedKnight -> 10,
      PromotedSilver -> 11,
      Horse          -> 12,
      Dragon         -> 13
    )
    val intToRole: Map[Int, Role] = roleToInt map { case (k, v) => v -> k }
  }

  private object Reader {
    private val maxPlies = 600

    def decode(bs: Seq[Byte], variant: Variant): Vector[Usi] =
      decode(bs, variant, maxPlies * 2)
    def decode(bs: Seq[Byte], variant: Variant, nb: Int): Vector[Usi] =
      decodeMovesAndDrops(bs take (nb * 2) map toInt, variant)

    private def decodeMovesAndDrops(mds: Seq[Int], variant: Variant): Vector[Usi] =
      mds.grouped(2)
        .map {
          case Seq(i1, i2) =>
            if (bitAt(i1, 7) && variant.supportsDrops)
              decodeDrop(i1, i2, variant)
            else decodeMove(i1, i2, variant)
          case x => !!(x map showByte mkString ",")
        }.toVector

    private def decodeMove(i1: Int, i2: Int, variant: Variant): Usi =
      Usi.Move(pos(right(i1, 7), variant.numberOfFiles), pos(right(i2, 7), variant.numberOfFiles), bitAt(i2, 7))

    private def decodeDrop(i1: Int, i2: Int, variant: Variant): Usi =
      Usi.Drop(Encoding.intToRole(right(i1, 7)), pos(right(i2, 7), variant.numberOfFiles))

    private def pos(i: Int, files: Int): Pos =
      Pos.at(i % files, i / files) getOrElse !!(
        s"Invalid position (files: $files, byte: ${showByte(i)})"
      )

    // right x bits
    private def right(i: Int, x: Int): Int = i & ((1 << x) - 1)
    // from right, starting at 0
    private def bitAt(i: Int, p: Int): Boolean = (i & (1 << p)) != 0

    private def !!(msg: String)          = throw new Exception("Binary usi reader failed: " + msg)
    private def showByte(b: Int): String = "%08d" format (b.toBinaryString.toInt)

    @inline private def toInt(b: Byte): Int = b & 0xff
  }

  private object Writer {

    def encode(usis: Seq[Usi], variant: Variant): Array[Byte] =
      usis.flatMap(encode(_, variant)).toArray

    private def encode(usi: Usi, variant: Variant): Seq[Byte] =
      usi match {
        case Usi.Move(orig, dest, prom) => encodeMove(orig, dest, prom, variant)
        case Usi.Drop(role, pos)        => encodeDrop(role, pos, variant)
      }

    private def encodeMove(orig: Pos, dest: Pos, prom: Boolean, variant: Variant): Seq[Byte] =
      Seq(
        posInt(orig, variant.numberOfFiles),
        (if (prom) (1 << 7) else 0) | posInt(dest, variant.numberOfFiles)
      ).map(_.toByte)

    private def encodeDrop(role: Role, pos: Pos, variant: Variant): Seq[Byte] =
      Seq(
        (1 << 7) | Encoding.roleToInt(role),
        posInt(pos, variant.numberOfFiles)
      ).map(_.toByte)

    private def posInt(pos: Pos, files: Int): Int =
      files * pos.rank.index + pos.file.index
  }

}
