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
import reactivemongo.api._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._

trait dsl {

  type Coll = reactivemongo.api.collections.bson.BSONCollection

  type QueryBuilder = GenericQueryBuilder[BSONSerializationPack.type]

  type BSONValueReader[A] = BSONReader[_ <: BSONValue, A]
  type BSONValueWriter[A] = BSONWriter[A, _ <: BSONValue]
  type BSONValueHandler[A] = BSONHandler[_ <: BSONValue, A]

  type BSONDocumentHandler[A] = BSONDocumentReader[A] with BSONDocumentWriter[A]

  implicit val LilaBSONDocumentZero: Zero[BSONDocument] =
    Zero.instance(BSONDocument())

  implicit def bsonDocumentToPretty(document: BSONDocument): String = {
    BSONDocument.pretty(document)
  }

  //**********************************************************************************************//
  // Helpers
  def $empty: BSONDocument = BSONDocument.empty

  def $doc(elements: Producer[BSONElement]*): BSONDocument = BSONDocument(elements: _*)

  def $doc(elements: Traversable[BSONElement]): BSONDocument = BSONDocument(elements)

  def $arr(elements: Producer[BSONValue]*): BSONArray = {
    BSONArray(elements: _*)
  }

  def $id[T](id: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): BSONDocument = BSONDocument("_id" -> id)

  def $inIds[T](ids: Iterable[T])(implicit writer: BSONWriter[T, _ <: BSONValue]): BSONDocument =
    BSONDocument("_id" -> $in(ids))

  def $boolean(b: Boolean) = BSONBoolean(b)
  def $string(s: String) = BSONString(s)

  // End of Helpers
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Logical Operators
  def $or(expressions: BSONDocument*): BSONDocument = {
    BSONDocument("$or" -> expressions)
  }

  def $and(expressions: BSONDocument*): BSONDocument = {
    BSONDocument("$and" -> expressions)
  }

  def $nor(expressions: BSONDocument*): BSONDocument = {
    BSONDocument("$nor" -> expressions)
  }
  // End of Top Level Logical Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Evaluation Operators
  def $text(search: String): BSONDocument = {
    BSONDocument("$text" -> BSONDocument("$search" -> search))
  }

  def $text(search: String, language: String): BSONDocument = {
    BSONDocument("$text" -> BSONDocument("$search" -> search, "$language" -> language))
  }

  def $where(expression: String): BSONDocument = {
    BSONDocument("$where" -> expression)
  }
  // End of Top Level Evaluation Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Field Update Operators
  def $inc(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    BSONDocument("$inc" -> BSONDocument((Seq(item) ++ items): _*))
  }
  def $inc(items: Iterable[BSONElement]): BSONDocument = {
    BSONDocument("$inc" -> BSONDocument(items))
  }

  def $mul(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$mul" -> BSONDocument(item))
  }

  def $rename(item: (String, String), items: (String, String)*)(implicit writer: BSONWriter[String, _ <: BSONValue]): BSONDocument = {
    BSONDocument("$rename" -> BSONDocument((Seq(item) ++ items).map(Producer.nameValue2Producer[String]): _*))
  }

  def $setOnInsert(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    BSONDocument("$setOnInsert" -> BSONDocument((Seq(item) ++ items): _*))
  }

  def $set(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    BSONDocument("$set" -> BSONDocument((Seq(item) ++ items): _*))
  }

  def $unset(field: String, fields: String*): BSONDocument = {
    BSONDocument("$unset" -> BSONDocument((Seq(field) ++ fields).map(_ -> BSONString(""))))
  }

  def $min(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$min" -> BSONDocument(item))
  }

  def $max(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$max" -> BSONDocument(item))
  }

  // Helpers
  def $eq[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$eq" -> value)

  def $gt[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$gt" -> value)

  /** Matches values that are greater than or equal to the value specified in the query. */
  def $gte[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$gte" -> value)

  /** Matches any of the values that exist in an array specified in the query.*/
  def $in[T](values: T*)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$in" -> values)

  /** Matches values that are less than the value specified in the query. */
  def $lt[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$lt" -> value)

  /** Matches values that are less than or equal to the value specified in the query. */
  def $lte[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$lte" -> value)

  /** Matches all values that are not equal to the value specified in the query. */
  def $ne[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): BSONValue = $doc("$ne" -> value)

  /** Matches values that do not exist in an array specified to the query. */
  def $nin[T](values: T*)(implicit writer: BSONWriter[T, _ <: BSONValue]) = $doc("$nin" -> values)

  trait CurrentDateValueProducer[T] {
    def produce: BSONValue
  }

  implicit class BooleanCurrentDateValueProducer(value: Boolean) extends CurrentDateValueProducer[Boolean] {
    def produce: BSONValue = BSONBoolean(value)
  }

  implicit class StringCurrentDateValueProducer(value: String) extends CurrentDateValueProducer[String] {
    def isValid: Boolean = Seq("date", "timestamp") contains value

    def produce: BSONValue = {
      if (!isValid)
        throw new IllegalArgumentException(value)

      BSONDocument("$type" -> value)
    }
  }

  def $currentDate(items: (String, CurrentDateValueProducer[_])*): BSONDocument = {
    BSONDocument("$currentDate" -> BSONDocument(items.map(item => item._1 -> item._2.produce)))
  }
  // End of Top Level Field Update Operators
  //**********************************************************************************************//

  //**********************************************************************************************//
  // Top Level Array Update Operators
  def $addToSet(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    BSONDocument("$addToSet" -> BSONDocument((Seq(item) ++ items): _*))
  }

  def $pop(item: (String, Int)): BSONDocument = {
    if (item._2 != -1 && item._2 != 1)
      throw new IllegalArgumentException(s"${item._2} is not equal to: -1 | 1")

    BSONDocument("$pop" -> BSONDocument(item))
  }

  def $push(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$push" -> BSONDocument(item))
  }

  def $pushEach[T](field: String, values: T*)(implicit writer: BSONWriter[T, _ <: BSONValue]): BSONDocument = {
    BSONDocument(
      "$push" -> BSONDocument(
        field -> BSONDocument(
          "$each" -> values
        )
      )
    )
  }

  def $pull(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$pull" -> BSONDocument(item))
  }
  // End ofTop Level Array Update Operators
  //**********************************************************************************************//

  /**
   * Represents the inital state of the expression which has only the name of the field.
   * It does not know the value of the expression.
   */
  trait ElementBuilder {
    def field: String
    def append(value: BSONDocument): BSONDocument = value
  }

  /** Represents the state of an expression which has a field and a value */
  trait Expression[V <: BSONValue] extends ElementBuilder {
    def value: V
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
  case class SimpleExpression[V <: BSONValue](field: String, value: V)
    extends Expression[V]

  /**
   * Expressions of this type can be cascaded. Examples:
   *
   * {{{
   *  "age" $gt 50 $lt 60
   *  "age" $gte 50 $lte 60
   * }}}
   *
   */
  case class CompositeExpression(field: String, value: BSONDocument)
      extends Expression[BSONDocument]
      with ComparisonOperators {
    override def append(value: BSONDocument): BSONDocument = {
      this.value ++ value
    }
  }

  /** MongoDB comparison operators. */
  trait ComparisonOperators { self: ElementBuilder =>

    def $eq[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): SimpleExpression[BSONValue] = {
      SimpleExpression(field, writer.write(value))
    }

    /** Matches values that are greater than the value specified in the query. */
    def $gt[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): CompositeExpression = {
      CompositeExpression(field, append(BSONDocument("$gt" -> value)))
    }

    /** Matches values that are greater than or equal to the value specified in the query. */
    def $gte[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): CompositeExpression = {
      CompositeExpression(field, append(BSONDocument("$gte" -> value)))
    }

    /** Matches any of the values that exist in an array specified in the query.*/
    def $in[T](values: T*)(implicit writer: BSONWriter[T, _ <: BSONValue]): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$in" -> values))
    }

    /** Matches values that are less than the value specified in the query. */
    def $lt[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): CompositeExpression = {
      CompositeExpression(field, append(BSONDocument("$lt" -> value)))
    }

    /** Matches values that are less than or equal to the value specified in the query. */
    def $lte[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): CompositeExpression = {
      CompositeExpression(field, append(BSONDocument("$lte" -> value)))
    }

    /** Matches all values that are not equal to the value specified in the query. */
    def $ne[T](value: T)(implicit writer: BSONWriter[T, _ <: BSONValue]): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$ne" -> value))
    }

    /** Matches values that do not exist in an array specified to the query. */
    def $nin[T](values: T*)(implicit writer: BSONWriter[T, _ <: BSONValue]): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$nin" -> values))
    }

  }

  trait LogicalOperators { self: ElementBuilder =>
    def $not(f: (String => Expression[BSONDocument])): SimpleExpression[BSONDocument] = {
      val expression = f(field)
      SimpleExpression(field, BSONDocument("$not" -> expression.value))
    }
  }

  trait ElementOperators { self: ElementBuilder =>
    def $exists(exists: Boolean): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$exists" -> exists))
    }
  }

  trait EvaluationOperators { self: ElementBuilder =>
    def $mod(divisor: Int, remainder: Int): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$mod" -> BSONArray(divisor, remainder)))
    }

    def $regex(value: String, options: String): SimpleExpression[BSONRegex] = {
      SimpleExpression(field, BSONRegex(value, options))
    }
  }

  trait ArrayOperators { self: ElementBuilder =>
    def $all[T](values: T*)(implicit writer: BSONWriter[T, _ <: BSONValue]): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$all" -> values))
    }

    def $elemMatch(query: Producer[BSONElement]*): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$elemMatch" -> BSONDocument(query: _*)))
    }

    def $size(size: Int): SimpleExpression[BSONDocument] = {
      SimpleExpression(field, BSONDocument("$size" -> size))
    }
  }

  object $sort {

    def asc(field: String) = $doc(field -> 1)
    def desc(field: String) = $doc(field -> -1)

    val naturalAsc = asc("$natural")
    val naturalDesc = desc("$natural")
    val naturalOrder = naturalDesc

    val createdAsc = asc("createdAt")
    val createdDesc = desc("createdAt")
    val updatedDesc = desc("updatedAt")
  }

  implicit class ElementBuilderLike(val field: String)
    extends ElementBuilder
    with ComparisonOperators
    with ElementOperators
    with EvaluationOperators
    with LogicalOperators
    with ArrayOperators

  implicit def toBSONElement[V <: BSONValue](expression: Expression[V])(implicit writer: BSONWriter[V, _ <: BSONValue]): Producer[BSONElement] = {
    expression.field -> expression.value
  }

  implicit def toBSONDocument[V <: BSONValue](expression: Expression[V])(implicit writer: BSONWriter[V, _ <: BSONValue]): BSONDocument =
    BSONDocument(expression.field -> expression.value)

}

object dsl extends dsl with CollExt
