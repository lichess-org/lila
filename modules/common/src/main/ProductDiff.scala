package lila.common

import lila.core.data.DiffStr

object ProductDiff:

  private def truncate(max: Int)(s: String): String =
    if s.length <= max then s else s.take(max) + "..."

  private def nestedProduct(value: Any): Option[Product] = value match
    case p: Product if !p.isInstanceOf[Option[?]] => Some(p)
    case Some(p: Product) => Some(p)
    case _ => None

  def apply[A <: Product](
      prev: Option[A],
      next: A,
      ignoredFields: Set[String] = Set.empty,
      nestedFields: Set[String] = Set.empty,
      maxLength: Int = 20_000
  ): Option[DiffStr] =
    val trunc = truncate(maxLength)

    def fieldLine(prefix: String)(name: String, value: Any) =
      s"$prefix $name: ${trunc(value.toString)}"

    def changedLine(name: String, prevVal: Any, nextVal: Any) =
      s"${fieldLine("-")(name, prevVal)}\n${fieldLine("+")(name, nextVal)}"

    def nestedDiff(name: String, prevVal: Any, nextVal: Any): List[String] =
      (nestedProduct(prevVal), nestedProduct(nextVal)) match
        case (Some(pv), Some(nv)) if pv.productArity == nv.productArity =>
          pv.productElementNames
            .zip(pv.productIterator)
            .zip(nv.productIterator)
            .collect:
              case ((k, a), b) if a != b => changedLine(s"$name.$k", a, b)
            .toList
        case _ => List(changedLine(name, prevVal, nextVal))

    def newLines(name: String, value: Any): List[String] =
      if value == None then Nil
      else if nestedFields(name)
      then
        nestedProduct(value).fold(List(fieldLine("+")(name, value))): p =>
          p.productElementNames
            .zip(p.productIterator)
            .collect:
              case (k, v) if v != None => fieldLine("+")(s"$name.$k", v)
            .toList
      else List(fieldLine("+")(name, value))

    val lineIterator = prev match
      case None =>
        next.productElementNames
          .zip(next.productIterator)
          .collect:
            case (name, value) if !ignoredFields(name) => newLines(name, value)
          .flatten
      case Some(old) =>
        old.productElementNames
          .zip(old.productIterator)
          .zip(next.productIterator)
          .flatMap:
            case ((name, _), _) if ignoredFields(name) => Nil
            case ((_, prevVal), nextVal) if prevVal == nextVal => Nil
            case ((name, prevVal), nextVal) if nestedFields(name) => nestedDiff(name, prevVal, nextVal)
            case ((name, prevVal), nextVal) => List(changedLine(name, prevVal, nextVal))

    lineIterator.mkString("\n").nonEmptyOption.map(DiffStr.apply)
