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

import scala.collection.Factory
import alleycats.Zero
import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.core.config.CollName

trait dsl:

  type ReadPref = ReadPref.type => ReadPreference
  object ReadPref:
    val pri: ReadPreference = ReadPreference.primary
    val sec: ReadPreference = ReadPreference.secondaryPreferred
    given Conversion[ReadPref, ReadPreference] = _(ReadPref)

  type Coll = reactivemongo.api.bson.collection.BSONCollection
  type Bdoc = BSONDocument
  type Barr = BSONArray

  def bsonWriteObjTry[A](a: A)(using writer: BSONDocumentWriter[A]) = writer.writeTry(a)
  def bsonWriteOpt[A](a: A)(using writer: BSONWriter[A]) = writer.writeOpt(a)
  def bsonWriteDoc[A](a: A)(using writer: BSONDocumentWriter[A]) = writer.writeOpt(a) | $empty

  // **********************************************************************************************//
  // Helpers
  val $empty: Bdoc = document.asStrict

  def $doc(elements: ElementProducer*): Bdoc = BSONDocument.strict(elements*)

  def $doc(elements: Iterable[(String, BSONValue)]): Bdoc = BSONDocument.strict(elements)

  def $arr(elements: Producer[BSONValue]*): Barr = BSONArray(elements*)

  def $id[T: BSONWriter](id: T): Bdoc = $doc("_id" -> id)

  def $inIds[T: BSONWriter](ids: Iterable[T]): Bdoc =
    $id($doc("$in" -> ids))

  def $boolean(b: Boolean) = BSONBoolean(b)
  def $string(s: String) = BSONString(s)
  def $string[A](a: A)(using sr: StringRuntime[A]) = BSONString(sr(a))
  def $int(i: Int) = BSONInteger(i)
  def $int[A](a: A)(using ir: IntRuntime[A]) = BSONInteger(ir(a))

  // End of Helpers
  // **********************************************************************************************//

  given Zero[Bdoc] = Zero($empty)

  // **********************************************************************************************//
  // Top Level Logical Operators
  def $or(expressions: Bdoc*): Bdoc = $doc("$or" -> expressions)
  def $and(expressions: Bdoc*): Bdoc = $doc("$and" -> expressions)
  def $nor(expressions: Bdoc*): Bdoc = $doc("$nor" -> expressions)
  def $not(expression: Bdoc): Bdoc = $doc("$not" -> expression)
  def $expr(expression: Bdoc): Bdoc = $doc("$expr" -> expression)

  // End of Top Level Logical Operators
  // **********************************************************************************************//

  // **********************************************************************************************//
  // Top Level Evaluation Operators
  def $text(term: String): Bdoc = $doc("$text" -> $doc("$search" -> term))
  def $text(term: String, lang: String): Bdoc = $doc:
    "$text" -> $doc("$search" -> term, f"$$language" -> lang)

  // End of Top Level Evaluation Operators
  // **********************************************************************************************//

  // **********************************************************************************************//
  // Top Level Field Update Operators
  def $inc(item: ElementProducer, items: ElementProducer*): Bdoc = $doc("$inc" -> $doc((Seq(item) ++ items)*))
  def $inc(doc: Bdoc): Bdoc = $doc("$inc" -> doc)

  def $mul(item: ElementProducer): Bdoc =
    $doc("$mul" -> $doc(item))

  def $set(items: ElementProducer*): Bdoc = $doc:
    "$set" -> items.nonEmpty.so($doc(items*))
  def $unset(fields: Iterable[String]): Bdoc = $doc:
    "$unset" -> fields.nonEmpty.so($doc(fields.map(k => (k, BSONString("")))))
  def $unset(field: String, fields: String*): Bdoc = $doc:
    "$unset" -> $doc((Seq(field) ++ fields).map(k => (k, BSONString(""))))

  def $unsetCompute[A](prev: A, next: A, fields: (String, A => Option[?])*): Bdoc =
    $unset:
      fields.flatMap: (key, accessor) =>
        (accessor(prev).isDefined && accessor(next).isEmpty).option(key)

  def $setBoolOrUnset(field: String, value: Boolean): Bdoc =
    if value then $set(field -> true) else $unset(field)
  def $setsAndUnsets(items: (String, Option[BSONValue])*): Bdoc =
    $set(items.collect { case (k, Some(v)) => k -> v }*) ++ $unset(items.collect { case (k, None) => k })
  def $min(item: ElementProducer): Bdoc = $doc("$min" -> $doc(item))
  def $max(item: ElementProducer): Bdoc = $doc("$max" -> $doc(item))
  def $divide[A: BSONWriter, B: BSONWriter](a: A, b: B): Bdoc = $doc("$divide" -> $arr(a, b))
  def $multiply[A: BSONWriter, B: BSONWriter](a: A, b: B): Bdoc = $doc("$multiply" -> $arr(a, b))

  // Helpers
  def $eq[T: BSONWriter](value: T) = $doc("$eq" -> value)
  def $gt[T: BSONWriter](value: T) = $doc("$gt" -> value)
  def $gte[T: BSONWriter](value: T) = $doc("$gte" -> value)
  def $in[T: BSONWriter](values: T*) = $doc("$in" -> values)
  def $lt[T: BSONWriter](value: T) = $doc("$lt" -> value)
  def $lte[T: BSONWriter](value: T) = $doc("$lte" -> value)
  def $ne[T: BSONWriter](value: T) = $doc("$ne" -> value)
  def $nin[T: BSONWriter](values: T*) = $doc("$nin" -> values)
  def $exists(value: Boolean) = $doc("$exists" -> value)

  // End of Top Level Field Update Operators
  // **********************************************************************************************//

  // **********************************************************************************************//
  // Top Level Array Update Operators

  def $addToSet(item: ElementProducer, items: ElementProducer*): Bdoc =
    $doc("$addToSet" -> $doc((Seq(item) ++ items)*))

  def $pop(item: (String, Int)): Bdoc =
    if item._2 != -1 && item._2 != 1 then
      throw new IllegalArgumentException(s"${item._2} is not equal to: -1 | 1")
    $doc("$pop" -> $doc(item))

  def $push(item: ElementProducer): Bdoc = $doc("$push" -> $doc(item))

  def $pushEach[T: BSONWriter](field: String, values: T*): Bdoc = $doc:
    "$push" -> $doc:
      field -> $doc:
        "$each" -> values

  def $pull(item: ElementProducer): Bdoc = $doc("$pull" -> $doc(item))

  def $addOrPull[T: BSONWriter](key: String, value: T, add: Boolean): Bdoc =
    $doc((if add then "$addToSet" else "$pull") -> $doc(key -> value))

  def $ifNull(expr: Bdoc, replacement: Bdoc): Bdoc =
    $doc("$ifNull" -> $arr(expr, replacement))

  // End ofTop Level Array Update Operators
  // **********************************************************************************************//

  /** Represents the initial state of the expression which has only the name of the field. It does not know
    * the value of the expression.
    */
  trait ElementBuilder:
    def field: String
    def append(value: Bdoc): Bdoc = value

  /** Represents the state of an expression which has a field and a value */
  trait Expression[V] extends ElementBuilder:
    def value: V
    def toBdoc(using BSONWriter[V]) = toBSONDocument(this)

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
      with ComparisonOperators:
    override def append(value: Bdoc): Bdoc =
      this.value ++ value

  /** MongoDB comparison operators. */
  trait ComparisonOperators:
    self: ElementBuilder =>

    def $eq[T: BSONWriter](value: T): SimpleExpression[BSONValue] =
      SimpleExpression(field, summon[BSONWriter[T]].writeTry(value).get)

    /** Matches values that are greater than the value specified in the query. */
    def $gt[T: BSONWriter](value: T): CompositeExpression =
      CompositeExpression(field, append($doc("$gt" -> value)))

    /** Matches values that are greater than or equal to the value specified in the query. */
    def $gte[T: BSONWriter](value: T): CompositeExpression =
      CompositeExpression(field, append($doc("$gte" -> value)))

    /** Matches any of the values that exist in an array specified in the query. */
    def $in[T: BSONWriter](values: Iterable[T]): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$in" -> values))

    /** Matches values that are less than the value specified in the query. */
    def $lt[T: BSONWriter](value: T): CompositeExpression =
      CompositeExpression(field, append($doc("$lt" -> value)))

    /** Matches values that are less than or equal to the value specified in the query. */
    def $lte[T: BSONWriter](value: T): CompositeExpression =
      CompositeExpression(field, append($doc("$lte" -> value)))

    def $inRange[T: BSONWriter](range: PairOf[T]): CompositeExpression =
      CompositeExpression(field, append($doc("$gte" -> range._1, "$lte" -> range._2)))

    /** Matches all values that are not equal to the value specified in the query. */
    def $ne[T: BSONWriter](value: T): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$ne" -> value))

    /** Matches values that do not exist in an array specified to the query. */
    def $nin[T: BSONWriter](values: Iterable[T]): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$nin" -> values))

  trait ElementOperators:
    self: ElementBuilder =>
    def $exists(v: Boolean): SimpleExpression[Bdoc] = SimpleExpression(field, $doc("$exists" -> v))

  trait EvaluationOperators:
    self: ElementBuilder =>
    def $mod(divisor: Int, remainder: Int): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$mod" -> BSONArray(divisor, remainder)))

    def $regex(value: String, options: String = ""): SimpleExpression[BSONRegex] =
      SimpleExpression(field, BSONRegex(value, options))

    def $startsWith(value: String, options: String = ""): SimpleExpression[BSONRegex] =
      $regex(s"^$value", options)

  trait ArrayOperators:
    self: ElementBuilder =>
    def $all[T: BSONWriter](values: Seq[T]): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$all" -> values))

    def $elemMatch(query: ElementProducer*): SimpleExpression[Bdoc] =
      SimpleExpression(field, $doc("$elemMatch" -> $doc(query*)))

    def $size(s: Int): SimpleExpression[Bdoc] = SimpleExpression(field, $doc("$size" -> s))

  def dateBetween(field: String, since: Option[Instant], until: Option[Instant]): Bdoc = (since, until) match
    case (Some(since), None) => field.$gte(since)
    case (None, Some(until)) => field.$lt(until)
    case (Some(since), Some(until)) => field.$gte(since).$lt(until)
    case _ => $empty

  object $sort:

    def asc(field: String) = $doc(field -> 1)
    def desc(field: String) = $doc(field -> -1)

    val naturalAsc = asc("$natural")
    val naturalDesc = desc("$natural")
    val naturalOrder = naturalDesc

    val createdAsc = asc("createdAt")
    val createdDesc = desc("createdAt")

    def orderField[A: BSONWriter](values: Iterable[A], field: String = "_id") =
      $doc("_order" -> $doc("$indexOfArray" -> $arr(values, "$" + field)))

  object $lookup:
    def pipelineFull(from: String, as: String, let: Bdoc, pipe: List[Bdoc]): Bdoc =
      $doc(
        "$lookup" -> $doc(
          "from" -> from,
          "as" -> as,
          "let" -> let,
          "pipeline" -> pipe
        )
      )

    // mongodb 5+ Correlated Subqueries Using Concise Syntax
    // https://www.mongodb.com/docs/manual/reference/operator/aggregation/lookup/#correlated-subqueries-using-concise-syntax
    def simple(from: CollName, as: String, local: String, foreign: String, pipe: List[Bdoc]): Bdoc =
      val lookup = $doc(
        "from" -> from.value,
        "as" -> as,
        "localField" -> local,
        "foreignField" -> foreign
      ) ++ pipe.nonEmpty.so($doc("pipeline" -> pipe))
      $doc("$lookup" -> lookup)

    def simple(from: Coll, as: String, local: String, foreign: String, pipe: List[Bdoc] = Nil): Bdoc =
      simple(CollName(from.name), as, local, foreign, pipe)

  implicit class ElementBuilderLike(val field: String)
      extends ElementBuilder
      with ComparisonOperators
      with ElementOperators
      with EvaluationOperators
      with ArrayOperators

  import scala.language.implicitConversions
  given toBSONDocument[V](using BSONWriter[V]): Conversion[Expression[V], Bdoc] =
    expression => $doc(expression.field -> expression.value)

  object toBSONValueOption:
    given [V](using w: BSONWriter[V]): Conversion[Option[V], Option[BSONValue]] =
      _.flatMap(w.writeOpt)

object dsl extends dsl with Handlers:

  extension [A](c: Cursor[A])(using Executor)

    // like collect, but with stopOnError defaulting to false
    def gather[M[_]](upTo: Int = Int.MaxValue)(using Factory[A, M[A]]): Fu[M[A]] =
      c.collect[M](upTo, Cursor.ContOnError[M[A]]())

    def list(limit: Int): Fu[List[A]] = gather[List](limit)
    def listAll(): Fu[List[A]] = gather[List](Int.MaxValue)

    // like headOption, but with stopOnError defaulting to false
    def uno: Fu[Option[A]] =
      c.collect[Iterable](1, Cursor.ContOnError[Iterable[A]]()).map(_.headOption)

    // extension [A](cursor: Cursor.WithOps[A])(using Executor)

    //   def gather[M[_]](upTo: Int)(using Factory[A, M[A]]): Fu[M[A]] =
    //     cursor.collect[M](upTo, Cursor.ContOnError[M[A]]())

    //   def list(): Fu[List[A]] =
    //     gather[List](Int.MaxValue)

    //   def list(limit: Int): Fu[List[A]] =
    //     gather[List](limit)

    //   def list(limit: Option[Int]): Fu[List[A]] =
    //     gather[List](limit | Int.MaxValue)

    def vector(limit: Int): Fu[Vector[A]] = gather[Vector](limit)

  import reactivemongo.api.WriteConcern as CWC

  extension (coll: Coll)(using @annotation.nowarn ex: Executor)

    def secondary = coll.withReadPreference(ReadPref.sec)

    def one[D: BSONDocumentReader](selector: Bdoc): Fu[Option[D]] =
      coll.find(selector, none[Bdoc]).one[D]

    def one[D: BSONDocumentReader](selector: Bdoc, projection: Bdoc): Fu[Option[D]] =
      coll.find(selector, projection.some).one[D]

    def list[D: BSONDocumentReader](
        selector: Bdoc,
        readPref: ReadPref = _.pri
    ): Fu[List[D]] =
      coll.find(selector, none[Bdoc]).cursor[D](readPref).list(Int.MaxValue)

    def list[D: BSONDocumentReader](selector: Bdoc, limit: Int): Fu[List[D]] =
      coll.find(selector, none[Bdoc]).cursor[D]().list(limit = limit)

    def byId[D](using BSONDocumentReader[D]): [I] => I => BSONWriter[I] ?=> Fu[Option[D]] =
      [I] =>
        (id: I) =>
          // coll.byId[D](id, projection) is treated by scala as byId[D](id -> projection)
          // because of reactivemongo given tuple2Writer
          // so we need to check if the ID writes to an array,
          // then the second value is probably a projection.
          summon[BSONWriter[I]]
            .writeOpt(id)
            .so:
              case BSONArray(Seq(id, proj: Bdoc)) => byIdProj[D](id, proj)
              case id => one[D]($id(id))

    def byIdProj[D](using BSONDocumentReader[D]): [I] => (I, Bdoc) => BSONWriter[I] ?=> Fu[Option[D]] =
      [I] => (id: I, projection: Bdoc) => one[D]($id(id), projection)

    def byIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        readPref: ReadPref = _.pri
    ): Fu[List[D]] =
      ids.nonEmpty.so(list[D]($inIds(ids), readPref))

    def byIdsProj[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Bdoc,
        readPref: ReadPref = _.pri
    ): Fu[List[D]] =
      ids.nonEmpty.so:
        coll.find($inIds(ids), projection.some).cursor[D](readPref).listAll()

    def byStringIds[D: BSONDocumentReader](
        ids: Iterable[String],
        readPref: ReadPref = _.pri
    ): Fu[List[D]] =
      byIds[D, String](ids, readPref)

    def countSel(selector: coll.pack.Document, limit: Option[Int] = none[Int]): Fu[Int] =
      coll
        .count(
          selector = selector.some,
          limit = limit,
          skip = 0,
          hint = None,
          readConcern = ReadConcern.Local
        )
        .dmap(_.toInt)

    def countAll: Fu[Long] =
      coll
        .count(
          selector = none,
          limit = None,
          skip = 0,
          hint = None,
          readConcern = ReadConcern.Local
        )

    def exists(selector: Bdoc): Fu[Boolean] = countSel(selector, 1.some).dmap(0 !=)

    def idsMap[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc] = None,
        readPref: ReadPref = _.pri
    )(docId: D => I): Fu[Map[I, D]] = ids.nonEmpty.so:
      projection
        .fold(coll.find($inIds(ids))): proj =>
          coll.find($inIds(ids), proj.some)
        .cursor[D](readPref)
        .collect[List](Int.MaxValue)
        .map(_.mapBy(docId))

    def byOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc] = None,
        readPref: ReadPref = _.pri
    )(docId: D => I): Fu[List[D]] = ids.nonEmpty.so:
      idsMap[D, I](ids, projection, readPref)(docId).map: m =>
        ids.view.flatMap(m.get).toList

    def optionsByOrderedIds[D: BSONDocumentReader, I: BSONWriter](
        ids: Iterable[I],
        projection: Option[Bdoc] = None,
        readPref: ReadPref = _.pri
    )(docId: D => I): Fu[List[Option[D]]] = ids.nonEmpty.so:
      idsMap[D, I](ids, projection, readPref)(docId).map: m =>
        ids.view.map(m.get).toList

    def primitive[V: BSONReader](selector: Bdoc, field: String): Fu[List[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .cursor[Bdoc]()
        .list(Int.MaxValue)
        .dmap:
          _.flatMap { _.getAsOpt[V](field) }

    def primitive[V: BSONReader](selector: Bdoc, sort: Bdoc, field: String): Fu[List[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .sort(sort)
        .cursor[Bdoc]()
        .list(Int.MaxValue)
        .dmap:
          _.flatMap { _.getAsOpt[V](field) }

    def primitive[V: BSONReader](selector: Bdoc, sort: Bdoc, nb: Int, field: String): Fu[List[V]] =
      (nb > 0).so:
        coll
          .find(selector, $doc(field -> true).some)
          .sort(sort)
          .cursor[Bdoc]()
          .list(nb)
          .dmap:
            _.flatMap { _.getAsOpt[V](field) }

    def primitiveOne[V: BSONReader](selector: Bdoc, field: String): Fu[Option[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .one[Bdoc]
        .dmap:
          _.flatMap { _.getAsOpt[V](field) }

    def primitiveOne[V: BSONReader](selector: Bdoc, sort: Bdoc, field: String): Fu[Option[V]] =
      coll
        .find(selector, $doc(field -> true).some)
        .sort(sort)
        .one[Bdoc]
        .dmap:
          _.flatMap { _.getAsOpt[V](field) }

    def primitiveMap[I: BSONReader: BSONWriter, V](
        ids: Iterable[I],
        field: String,
        fieldExtractor: Bdoc => Option[V]
    ): Fu[Map[I, V]] =
      coll
        .find($inIds(ids), $doc(field -> true).some)
        .cursor[Bdoc]()
        .list(Int.MaxValue)
        .dmap:
          _.flatMap: obj =>
            obj
              .getAsOpt[I]("_id")
              .flatMap: id =>
                fieldExtractor(obj).map { id -> _ }
          .toMap

    def updateUnchecked(selector: Bdoc, set: Bdoc): Unit =
      coll
        .update(ordered = false, writeConcern = WriteConcern.Unacknowledged)
        .one(selector, set)

    def updateField[V: BSONWriter](selector: Bdoc, field: String, value: V) =
      coll.update.one(selector, $set(field -> value))

    def updateFieldUnchecked[V: BSONWriter](selector: Bdoc, field: String, value: V): Unit =
      updateUnchecked(selector, $set(field -> value))

    def incField(selector: Bdoc, field: String, value: Int = 1) =
      coll.update.one(selector, $inc(field -> value))

    def incFieldUnchecked(selector: Bdoc, field: String, value: Int = 1): Unit =
      updateUnchecked(selector, $inc(field -> value))

    def unsetField(selector: Bdoc, field: String, multi: Boolean = false) =
      coll.update.one(selector, $unset(field), multi = multi)

    def updateOrUnsetField[V: BSONWriter](selector: Bdoc, field: String, value: Option[V]): Fu[Int] =
      value match
        case None => unsetField(selector, field).dmap(_.n)
        case Some(v) => updateField(selector, field, v).dmap(_.n)

    def fetchUpdate[D: BSONDocumentHandler](selector: Bdoc)(update: D => Bdoc): Funit =
      one[D](selector).flatMapz: doc =>
        coll.update.one(selector, update(doc)).void

    def aggregateList(
        maxDocs: Int, // can actually return more documents (?)
        readPref: ReadPref = _.pri,
        allowDiskUse: Boolean = false
    )(
        f: coll.AggregationFramework => AggregationPipeline[coll.PipelineOperator]
    )(using CursorProducer[Bdoc]): Fu[List[Bdoc]] =
      coll
        .aggregateWith[Bdoc](
          allowDiskUse = allowDiskUse,
          readPreference = readPref
        ): agg =>
          val (head, tail) = f(agg)
          head :: tail
        .collect[List](maxDocs = maxDocs)

    def aggregateOne(
        readPref: ReadPref = _.pri,
        allowDiskUse: Boolean = false
    )(
        f: coll.AggregationFramework => AggregationPipeline[coll.PipelineOperator]
    )(using cp: CursorProducer[Bdoc]): Fu[Option[Bdoc]] =
      coll
        .aggregateWith[Bdoc](
          allowDiskUse = allowDiskUse,
          readPreference = readPref
        ): agg =>
          val nonEmpty = f(agg)
          nonEmpty._1 +: nonEmpty._2
        .collect[List](maxDocs = 1)
        .dmap(_.headOption) // .one[Bdoc] ?

    def aggregateExists(
        readPref: ReadPref = _.pri,
        allowDiskUse: Boolean = false
    )(
        f: coll.AggregationFramework => AggregationPipeline[coll.PipelineOperator]
    )(using cp: CursorProducer[Bdoc]): Fu[Boolean] =
      coll
        .aggregateWith[Bdoc](
          allowDiskUse = allowDiskUse,
          readPreference = readPref
        ): agg =>
          val nonEmpty = f(agg)
          nonEmpty._1 +: nonEmpty._2
        .headOption
        .dmap(_.isDefined)

    def distinctEasy[T, M[_] <: Iterable[?]](
        key: String,
        selector: coll.pack.Document,
        readPref: ReadPref = _.pri
    )(using
        reader: coll.pack.NarrowValueReader[T],
        cbf: Factory[T, M[T]]
    ): Fu[M[T]] =
      coll.withReadPreference(readPref).distinct(key, selector.some, ReadConcern.Local, None)

    def findAndUpdateSimplified[D: BSONDocumentReader](
        selector: coll.pack.Document,
        update: coll.pack.Document,
        fetchNewObject: Boolean = false,
        upsert: Boolean = false,
        sort: Option[coll.pack.Document] = None,
        fields: Option[coll.pack.Document] = None,
        writeConcern: CWC = CWC.Acknowledged
    ): Fu[Option[D]] =
      coll
        .findAndUpdate(
          selector = selector,
          update = update,
          fetchNewObject = fetchNewObject,
          upsert = upsert,
          sort = sort,
          fields = fields,
          bypassDocumentValidation = false,
          writeConcern = writeConcern,
          maxTime = none,
          collation = none,
          arrayFilters = Seq.empty
        )
        .map:
          _.value.flatMap(summon[BSONDocumentReader[D]].readOpt)

    def findAndRemove[D: BSONDocumentReader](
        selector: coll.pack.Document,
        sort: Option[coll.pack.Document] = None,
        fields: Option[coll.pack.Document] = None,
        writeConcern: CWC = CWC.Acknowledged
    ): Fu[Option[D]] =
      coll
        .findAndRemove(
          selector = selector,
          sort = sort,
          fields = fields,
          writeConcern = writeConcern,
          maxTime = none,
          collation = none,
          arrayFilters = Seq.empty
        )
        .map:
          _.value.flatMap(summon[BSONDocumentReader[D]].readOpt)
