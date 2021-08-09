// Copyright (C) 2014 Fehmi Can Saglam (@fehmicans) and contributors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package lila.db

import ornicar.scalalib.Zero

import reactivemongo.api.bson._

trait dsl {

  type Coll = reactivemongo.api.bson.collection.BSONCollection
  type Bdoc = BSONDocument
  type Barr = BSONArray

  //**********************************************************************************************//
  // Helpers
  val $empty: Bdoc = document.asStrict

  def $doc(elements: ElementProducer*): Bdoc = BSONDocument.strict(elements: _*)

  def $doc(elements: Iterable[(String, BSONValue)]): Bdoc = BSONDocument.strict(elements)

  def $arr(elements: Producer[BSONValue]*): Barr = BSONArray(elements: _*)

  def $id[T: BSONWriter](id: T): Bdoc = $doc("_id" -> id)

  def $inIds[T: BSONWriter](ids: Iterable[T]): Bdoc =
    $id($doc("$in" -> ids))

  def $boolean(b: Boolean) = BSONBoolean(b)
  def $string(s: String)   = BSONString(s)
  def $int(i: Int)         = BSONInteger(i)

  // End of Helpers
  //**********************************************************************************************//

  implicit val LilaBSONDocumentZero: Zero[Bdoc] = Zero.instance($empty)

  //**********************************************************************************************//
  // Top Level Logical Operators
  def $or(expressions: Bdoc*): Bdoc = {
    $doc("$or" -> expressions)
  }

  def $and(expressions: Bdoc*): Bdoc = {
    $doc("$and" -> expressions)
  }

  def $nor(expressions: Bdoc*): Bdoc = {
    $doc("$nor" -> expressions)
  }
  // End of Top Level Logical Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Evaluation Operators
  def $text(term: String): Bdoc = {
    $doc("$text" -> $doc("$search" -> term))
  }

  def $text(term: String, lang: String): Bdoc = {
    $doc("$text" -> $doc("$search" -> term, f"$$language" -> lang))
  }

  def $where(expr: String): Bdoc = {
    $doc("$where" -> expr)
  }
  // End of Top Level Evaluation Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Field Update Operators
  def $inc(item: ElementProducer, items: ElementProducer*): Bdoc = {
    $doc("$inc" -> $doc((Seq(item) ++ items): _*))
  }
  def $inc(doc: Bdoc): Bdoc =
    $doc("$inc" -> doc)

  def $mul(item: ElementProducer): Bdoc = {
    $doc("$mul" -> $doc(item))
  }

  def $setOnInsert(item: ElementProducer, items: ElementProducer*): Bdoc = {
    $doc("$setOnInsert" -> $doc((Seq(item) ++ items): _*))
  }

  def $set(item: ElementProducer, items: ElementProducer*): Bdoc = {
    $doc("$set" -> $doc((Seq(item) ++ items): _*))
  }

  def $unset(field: String, fields: String*): Bdoc = {
    $doc("$unset" -> $doc((Seq(field) ++ fields).map(k => (k, BSONString("")))))
  }

  def $unset(fields: Seq[String]): Bdoc =
    fields.nonEmpty ?? {
      $doc("$unset" -> $doc(fields.map(k => (k, BSONString("")))))
    }

  def $setBoolOrUnset(field: String, value: Boolean): Bdoc = {
    if (value) $set(field -> true) else $unset(field)
  }

  def $min(item: ElementProducer): Bdoc = {
    $doc("$min" -> $doc(item))
  }

  def $max(item: ElementProducer): Bdoc = {
    $doc("$max" -> $doc(item))
  }

  // Helpers
  def $eq[T: BSONWriter](value: T) = $doc("$eq" -> value)

  def $gt[T: BSONWriter](value: T) = $doc("$gt" -> value)

  /** Matches values that are greater than or equal to the value specified in the query. */
  def $gte[T: BSONWriter](value: T) = $doc("$gte" -> value)

  /** Matches any of the values that exist in an array specified in the query. */
  def $in[T: BSONWriter](values: T*) = $doc("$in" -> values)

  /** Matches values that are less than the value specified in the query. */
  def $lt[T: BSONWriter](value: T) = $doc("$lt" -> value)

  /** Matches values that are less than or equal to the value specified in the query. */
  def $lte[T: BSONWriter](value: T) = $doc("$lte" -> value)

  /** Matches all values that are not equal to the value specified in the query. */
  def $ne[T: BSONWriter](value: T) = $doc("$ne" -> value)

  /** Matches values that do not exist in an array specified to the query. */
  def $nin[T: BSONWriter](values: T*) = $doc("$nin" -> values)

  def $exists(value: Boolean) = $doc("$exists" -> value)

  trait CurrentDateValueProducer[T] {
    def produce: BSONValue
  }

  implicit final class BooleanCurrentDateValueProducer(value: Boolean)
      extends CurrentDateValueProducer[Boolean] {
    def produce: BSONValue = BSONBoolean(value)
  }

  implicit final class StringCurrentDateValueProducer(value: String)
      extends CurrentDateValueProducer[String] {
    def isValid: Boolean = Seq("date", "timestamp") contains value

    def produce: BSONValue = {
      if (!isValid)
        throw new IllegalArgumentException(value)

      $doc("$type" -> value)
    }
  }

  // End of Top Level Field Update Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Array Update Operators

  def $addToSet(item: ElementProducer, items: ElementProducer*): Bdoc =
    $doc("$addToSet" -> $doc((Seq(item) ++ items): _*))

  def $pop(item: (String, Int)): Bdoc = {
    if (item._2 != -1 && item._2 != 1)
      throw new IllegalArgumentException(s"${item._2} is not equal to: -1 | 1")
    $doc("$pop" -> $doc(item))
  }

  def $push(item: ElementProducer): Bdoc =
    $doc("$push" -> $doc(item))

  def $pushEach[T: BSONWriter](field: String, values: T*): Bdoc =
    $doc(
      "$push" -> $doc(
        field -> $doc(
          "$each" -> values
        )
      )
    )

  def $pull(item: ElementProducer): Bdoc =
    $doc("$pull" -> $doc(item))

  def $addOrPull[T: BSONWriter](key: String, value: T, add: Boolean): Bdoc =
    $doc((if (add) "$addToSet" else "$pull") -> $doc(key -> value))

  // End ofTop Level Array Update Operators
  //**********************************************************************************************//

  /** Represents the initial state of the expression which has only the name of the field.
    * It does not know the value of the expression.
    */
  trait ElementBuilder {
    def field: String
    def append(value: Bdoc): Bdoc = value
  }

  /** Represents the state of an expression which has a field and a value */
  trait Expression[V] extends ElementBuilder {
    def value: V
    def toBdoc(implicit writer: BSONWriter[V]) = toBSONDocument(this)
  }

  /*
   * This type of expressions cannot be cascaded. Examples:
   *
   * {{{
   * "price" $eq 10
   * "price" $ne 1000
   * "size" $in ("S", "M", "L")
   * "size" $nin ("S", "XXL")
   * }}}
   *
   */
  case class SimpleExpression[V <: BSONValue](field: String, value: V) extends Expression[V]

  /** Expressions of this type can be cascaded. Examples:
    *
    * {{{
    *  "age" $gt 50 $lt 60
    *  "age" $gte 50 $lte 60
    * }}}
    */
  case class CompositeExpression(field: String, value: Bdoc)
      extends Expression[Bdoc]
      with ComparisonOperators {
    override def append(value: Bdoc): Bdoc = {
      this.value ++ value
    }
  }

  /** MongoDB comparison operators. */
  trait ComparisonOperators { self: ElementBuilder =>

    def $eq[T: BSONWriter](value: T): SimpleExpression[BSONValue] = {
      SimpleExpression(field, implicitly[BSONWriter[T]].writeTry(value).get)
    }

    /** Matches values that are greater than the value specified in the query. */
    def $gt[T: BSONWriter](value: T): CompositeExpression = {
      CompositeExpression(field, append($doc("$gt" -> value)))
    }

    /** Matches values that are greater than or equal to the value specified in the query. */
    def $gte[T: BSONWriter](value: T): CompositeExpression = {
      CompositeExpression(field, append($doc("$gte" -> value)))
    }

    /** Matches any of the values that exist in an array specified in the query. */
    def $in[T: BSONWriter](values: Iterable[T]): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$in" -> values))
    }

    /** Matches values that are less than the value specified in the query. */
    def $lt[T: BSONWriter](value: T): CompositeExpression = {
      CompositeExpression(field, append($doc("$lt" -> value)))
    }

    /** Matches values that are less than or equal to the value specified in the query. */
    def $lte[T: BSONWriter](value: T): CompositeExpression = {
      CompositeExpression(field, append($doc("$lte" -> value)))
    }

    /** Matches all values that are not equal to the value specified in the query. */
    def $ne[T: BSONWriter](value: T): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$ne" -> value))
    }

    /** Matches values that do not exist in an array specified to the query. */
    def $nin[T: BSONWriter](values: Iterable[T]): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$nin" -> values))
    }

  }

  trait LogicalOperators { self: ElementBuilder =>
    def $not(f: String => Expression[Bdoc]): SimpleExpression[Bdoc] = {
      val expression = f(field)
      SimpleExpression(field, $doc("$not" -> expression.value))
    }
  }

  trait ElementOperators { self: ElementBuilder =>
    def $exists(v: Boolean): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$exists" -> v))
    }
  }

  trait EvaluationOperators { self: ElementBuilder =>
    def $mod(divisor: Int, remainder: Int): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$mod" -> BSONArray(divisor, remainder)))

    def $regex(value: String, options: String = ""): SimpleExpression[BSONRegex] =
      SimpleExpression(field, BSONRegex(value, options))

    def $startsWith(value: String, options: String = ""): SimpleExpression[BSONRegex] =
      $regex(s"^$value", options)
  }

  trait ArrayOperators { self: ElementBuilder =>
    def $all[T: BSONWriter](values: Seq[T]): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$all" -> values))
    }

    def $elemMatch(query: ElementProducer*): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$elemMatch" -> $doc(query: _*)))
    }

    def $size(s: Int): SimpleExpression[Bdoc] = {
      SimpleExpression(field, $doc("$size" -> s))
    }
  }

  object $sort {

    def asc(field: String)  = $doc(field -> 1)
    def desc(field: String) = $doc(field -> -1)

    val naturalAsc   = asc("$natural")
    val naturalDesc  = desc("$natural")
    val naturalOrder = naturalDesc

    val createdAsc  = asc("createdAt")
    val createdDesc = desc("createdAt")
  }

  object $lookup {
    def simple(from: String, as: String, local: String, foreign: String): Bdoc = $doc(
      "$lookup" -> $doc(
        "from"         -> from,
        "as"           -> as,
        "localField"   -> local,
        "foreignField" -> foreign
      )
    )
    def simple(from: Coll, as: String, local: String, foreign: String): Bdoc =
      simple(from.name, as, local, foreign)
    def simple(from: AsyncColl, as: String, local: String, foreign: String): Bdoc =
      simple(from.name.value, as, local, foreign)
    def pipeline(from: Coll, as: String, local: String, foreign: String, pipeline: List[Bdoc]): Bdoc =
      $doc(
        "$lookup" -> $doc(
          "from" -> from.name,
          "as"   -> as,
          "let"  -> $doc("local" -> s"$$$local"),
          "pipeline" -> {
            $doc(
              "$match" -> $doc(
                "$expr" -> $doc($doc("$eq" -> $arr(s"$$$foreign", "$$local")))
              )
            ) :: pipeline
          }
        )
      )
  }

  implicit class ElementBuilderLike(val field: String)
      extends ElementBuilder
      with ComparisonOperators
      with ElementOperators
      with EvaluationOperators
      with LogicalOperators
      with ArrayOperators

  implicit def toBSONDocument[V: BSONWriter](expression: Expression[V]): Bdoc =
    $doc(expression.field -> expression.value)

}

// sealed trait LowPriorityDsl { self: dsl =>
//   // Priority lower than toBSONDocument
//   implicit def toBSONElement[V <: BSONValue](expression: Expression[V])(implicit writer: BSONWriter[V, _ <: BSONValue]): ElementProducer = {
//     BSONElement(expression.field, expression.value)
//   }
// }

object dsl extends dsl with CollExt with QueryBuilderExt with CursorExt with Handlers
