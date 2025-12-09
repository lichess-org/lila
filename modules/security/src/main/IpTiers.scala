package lila.security

import java.nio.file.Files
import play.api.data.*
import play.api.data.format.Formatter

import lila.common.config.GetRelativeFile

// reads and writes an nginx ip tier config file like:
// {ip_address} {tier_number}; # {some comment including contact info}
// 1.1.1.1 2; # contact info
final class IpTiers(getFile: GetRelativeFile)(using Executor):

  private val filePath = "data/ip-tiers.txt"
  private def fullPath = getFile.exec(filePath).toPath

  case class Line(ip: String, tier: Int, comment: String):
    override def toString: String = s"$ip ${" " * (20 - ip.size)}$tier; # $comment"

  def get: Fu[Either[String, List[Line]]] = Future:
    val strs = Files.readString(fullPath).linesIterator.toList
    Right(strs.flatMap(str => parse(str).toOption))
  .recover:
    case _: java.nio.file.NoSuchFileException => Left(s"File $filePath not found")

  def form: Fu[Either[String, Form[List[Line]]]] = get.map:
    _.map: current =>
      Form(Forms.single("list" -> Forms.of[List[Line]])).fill(current)

  def set(newLines: List[Line]): Funit = Future:
    Files.writeString(fullPath, newLines.mkString("\n"))

  private object parse:
    val regex = """([\w\.:/]+) +(\d+); *#(.+)""".r
    def apply(line: String): Either[String, Line] = line match
      case regex(ipStr, tierStr, comment) =>
        for tier <- tierStr.toIntOption.filter(_ >= 1).filter(_ <= 3).toRight(s"Invalid tier: $tierStr")
        yield Line(ipStr, tier, comment.trim)
      case str => Left(s"Invalid line format: $str")

  private given Formatter[List[Line]] =
    lila.common.Form.formatter.stringTryFormatter(
      str =>
        val strs = str.linesIterator.map(_.trim).filter(_.nonEmpty).zipWithIndex.toList
        val parsed = strs.map((l, i) => parse(l).left.map(_ -> i))
        val errors = parsed.collect { case Left(err, i) => s"l.${i + 1}: $err" }
        if errors.nonEmpty then Left(errors.mkString("\n"))
        else
          val allLines = parsed.collect { case Right(line) => line }
          val dups = allLines
            .groupBy(_.ip)
            .collect:
              case (ip, lines) if lines.sizeIs > 1 => s"Duplicate IP address $ip"
          if dups.nonEmpty then Left(dups.mkString("\n"))
          else Right(allLines)
      ,
      _.mkString("\n")
    )
