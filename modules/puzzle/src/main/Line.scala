package lila.puzzle

import play.api.libs.json._

sealed trait Line

case class Node(move: String, lines: Lines) extends Line
case class Win(move: String)                extends Line
case class Retry(move: String)              extends Line

object Line {

  def minDepth(lines: Lines): Int = {
    @scala.annotation.tailrec
    def walk(subs: Vector[(Lines, Int)]): Option[Int] =
      subs match {
        case (lines, depth) +: rest =>
          lines match {
            case Nil                  => walk(rest)
            case Win(_) :: _          => depth.some
            case Retry(_) :: siblings => walk(rest :+ (siblings -> depth))
            case Node(_, children) :: siblings =>
              walk(rest :+ (siblings -> depth) :+ (children -> (depth + 1)))
          }
        case _ => none
      }
    (1 + ~walk(Vector(lines -> 1))) / 2
  }

  def solution(lines: Lines): List[String] = {

    def getIn(lines: Lines, path: List[String]): Lines =
      path match {
        case Nil => lines
        case head :: rest =>
          lines collectFirst {
            case Node(move, lines) if move == head => getIn(lines, rest)
            case w @ Win(move) if move == head     => List(w)
            case r @ Retry(move) if move == head   => List(r)
          } getOrElse Nil
      }

    def loop(paths: List[List[String]]): List[String] =
      paths match {
        case Nil => Nil
        case path :: siblings =>
          getIn(lines, path) match {
            case List(Win(m))   => path :+ m
            case List(Retry(_)) => loop(siblings)
            case ahead =>
              ahead.collectFirst { case Win(m) =>
                path :+ m
              } | {
                val children = ahead collect { case Node(m, _) => path :+ m }
                loop(siblings ::: children)
              }
          }
      }

    lines.collectFirst { case Win(move) =>
      List(move)
    } | loop(lines collect { case Node(move, _) =>
      List(move)
    })

  }

  def toString(lines: Lines, level: Int = 0): String = {
    val indent = ". " * level
    lines map {
      case Win(move)        => s"$indent$move win"
      case Retry(move)      => s"$indent$move retry"
      case Node(move, more) => s"$indent$move\n${toString(more, level + 1)}"
    } mkString "\n"
  }

  def toJson(lines: Lines): JsObject =
    JsObject(lines map {
      case Win(move)        => move -> JsString("win")
      case Retry(move)      => move -> JsString("retry")
      case Node(move, more) => move -> toJson(more)
    })
}
