// credit: https://github.com/saksdirect/gfc-semver/blob/master/src/main/scala/com/gilt/gfc/semver/SemVer.scala
package com.gilt.gfc.semver

object SemVer:
  def apply(version: String): SemVer =
    val bits = version.split("[\\.\\-]")
    val (nums, extras) =
      bits.take(3).foldLeft((Nil: List[Long], Nil: List[String])) { case ((num, extra), bit) =>
        import scala.util.control.Exception.*
        allCatch.opt(bit.toLong) match
          case Some(long) => (long :: num, extra)
          case None => (num, bit :: extra)
      }
    nums.reverse match
      case x :: y :: z :: Nil =>
        SemVer(
          x,
          y,
          z, {
            val e = extras.reverse ::: bits.drop(3).toList
            if e.isEmpty then None else Some(e.mkString("-"))
          },
          version
        )
      case x :: y :: Nil =>
        SemVer(
          x,
          y,
          0, {
            val e = extras.reverse ::: bits.drop(2).toList
            if e.isEmpty then None else Some(e.mkString("-"))
          },
          version
        )
      case x :: Nil =>
        SemVer(
          x,
          0,
          0, {
            val e = extras.reverse ::: bits.drop(1).toList
            if e.isEmpty then None else Some(e.mkString("-"))
          },
          version
        )
      case _ =>
        sys.error("Cannot parse version: [%s]".format(version))

  val Snapshot = "-SNAPSHOT"
  def isSnapshotVersion(v: String): Boolean = v.trim.endsWith(Snapshot)

  def isIntegrationVersion(v: String): Boolean =
    val Rx = """(\d+).(\d+).(\d+).(\d{14})""".r
    v match
      case Rx(_, _, _, _) => true
      case _ => false

  def isReleaseVersion(v: String): Boolean = !isSnapshotVersion(v) && !isIntegrationVersion(v)

case class SemVer(major: Long, minor: Long, point: Long, extra: Option[String], original: String)
    extends Ordered[SemVer]:

  override def equals(obj: Any): Boolean =
    obj match
      case version: SemVer => this.compareTo(version) == 0
      case _ => false

  def compare(o: SemVer): Int =
    if major != o.major then major.compare(o.major)
    else if minor != o.minor then minor.compare(o.minor)
    else if point != o.point then point.compare(o.point)
    else
      import scala.util.control.Exception.*
      val thsNumPrefix: Option[Long] = allCatch.opt(extra.get.takeWhile(_.isDigit).toLong)
      val thtNumPrefix: Option[Long] = allCatch.opt(o.extra.get.takeWhile(_.isDigit).toLong)
      (extra, thsNumPrefix, o.extra, thtNumPrefix) match
        case (Some(ths), _, Some(tht), _) if ths == "SNAPSHOT" || tht == "SNAPSHOT" =>
          0 // At least one SNAPSHOT: Can't decide
        case (Some(_), Some(thsNum), Some(_), Some(thtNum)) if thsNum < thtNum =>
          -1 // Number prefixes compared
        case (Some(_), Some(thsNum), Some(_), Some(thtNum)) if thsNum > thtNum =>
          1 // Number prefixes compared
        case (Some(ths), Some(_), Some(tht), Some(_)) =>
          ths.compareTo(tht) // Number prefixes same: Compare lexicographically
        case (Some(_), Some(_), Some(_), None) => 0 // One starts with number the other doesn't: Can't decide
        case (Some(_), None, Some(_), Some(_)) => 0 // One starts with number the other doesn't: Can't decide
        case (Some(ths), None, Some(tht), None) =>
          ths.compareTo(tht) // No number prefixes: Compare lexicographically
        case (Some(_), _, None, _) => -1 // One has extra, the other doesn't
        case (None, _, Some(_), _) => 1 // One has extra, the other doesn't
        case _ => 0 // Both have no extra: They are the same

  def isSnapshotVersion = SemVer.isSnapshotVersion(this.original)
  def isIntegrationVersion = SemVer.isIntegrationVersion(this.original)
  def isReleaseVersion = SemVer.isReleaseVersion(this.original)
