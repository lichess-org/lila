package lila.report

import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class AutomodRepo(val coll: Coll)(using Executor):

  import Automod.*

  private given BSONHandler[JobType] = tryHandler(
    _.asOpt[String]
      .flatMap(name => JobType.values.find(_.toString == name))
      .toTry("bad jobType"),
    jobType => BSONString(jobType.toString)
  )
  private given BSONHandler[Response.Result] = tryHandler(
    _.asOpt[String]
      .flatMap(name => Response.Result.values.find(_.toString == name))
      .toTry("bad response result"),
    result => BSONString(result.toString)
  )
  private given BSONDocumentHandler[Source] = Macros.handler
  private given BSONDocumentHandler[Usage] = Macros.handler
  private given BSONDocumentHandler[Response] = Macros.handler
  private given BSONDocumentHandler[Transaction] = Macros.handler

  def create(request: Request): Fu[Transaction] =
    val transaction = Transaction(
      id = request.id,
      jobType = request.job.jobType,
      config = request.job.config,
      source = request.job.source,
      model = request.model,
      updated = nowInstant
    )
    coll.update
      .one(
        $doc("_id" -> transaction.id),
        $set(
          "jobType" -> transaction.jobType,
          "config" -> transaction.config,
          "source" -> transaction.source,
          "model" -> transaction.model,
          "updated" -> transaction.updated
        ) ++ $unset("response") ++ $inc("failures" -> 0),
        upsert = true
      )
      .inject(transaction)

  def respond(transaction: Transaction, response: Response): Funit =
    coll.update
      .one(
        $doc("_id" -> transaction.id),
        $set("response" -> response, "updated" -> nowInstant) ++ response.result.match
          case Response.Result.failed => $inc("failures" -> 1)
          case _ => $empty
      )
      .void

  def failed(jobType: JobType, offset: Int, limit: Int): Fu[List[Transaction]] =
    coll
      .find(activeFailures(jobType), Option.empty[Bdoc])
      .sort($sort.desc("response.date"))
      .skip(offset)
      .cursor[Transaction]()
      .list(limit)

  def failedCount(jobType: JobType): Fu[Int] = coll.countSel(activeFailures(jobType))

  def failedSourceIds(jobType: JobType): Fu[List[String]] =
    coll.distinctEasy[String, List]("source.id", activeFailures(jobType) ++ $doc("source.id".$exists(true)))

  def clear(jobType: JobType, sourceId: String): Funit =
    clear(activeFailures(jobType) ++ $doc("source.id" -> sourceId))

  def clearAll(jobType: JobType): Funit = clear(activeFailures(jobType))

  def statusJson(adminLinks: List[AutomodRepo.AdminLink], recent: Int = 10): Fu[JsObject] =
    for
      transactions <- coll
        .find($empty, Option.empty[Bdoc])
        .sort($sort.desc("updated"))
        .cursor[Transaction]()
        .list(recent)
      jobStats <- coll.aggregateList(Int.MaxValue): framework =>
        import framework.*
        Match($doc("updated".$gte(nowInstant.minusDays(30)))) -> List(
          Sort(Descending("updated")),
          PipelineOperator(
            $doc(
              "$group" -> $doc(
                "_id" -> $doc("jobType" -> "$jobType", "model" -> "$model"),
                "responses" -> $doc("$push" -> "$response.result"),
                "inputTokens" -> $doc("$sum" -> "$response.usage.inputTokens"),
                "outputTokens" -> $doc("$sum" -> "$response.usage.outputTokens")
              )
            )
          ),
          Project(
            $doc(
              "responses" -> $doc("$slice" -> $arr("$responses", recent)),
              "inputTokens" -> 1,
              "outputTokens" -> 1
            )
          )
        )
      pending <- coll.countSel($doc("response".$exists(false)))
    yield
      val jobsByTypeAndModel = jobStats.flatMap: stat =>
        for
          id <- stat.getAsOpt[Bdoc]("_id")
          jobType <- id.getAsOpt[String]("jobType")
          model <- id.getAsOpt[String]("model")
          responses <- stat.getAsOpt[BSONArray]("responses")
        yield Json.obj(
          "jobType" -> jobType,
          "model" -> model,
          "inputTokens" -> stat.getAsOpt[Long]("inputTokens").getOrElse(0L),
          "outputTokens" -> stat.getAsOpt[Long]("outputTokens").getOrElse(0L),
          "recent" -> responses.size,
          "failures" -> responses.values.count(_ == BSONString(Response.Result.failed.toString))
        )
      Json.obj(
        "recentJobs" -> transactions.map(transactionJson),
        "jobsByTypeAndModel" -> jobsByTypeAndModel,
        "pending" -> pending,
        "adminLinks" -> Json.obj(adminLinks.map(link => link.jobType.toString -> link.url)*)
      )

  private def clear(selector: Bdoc): Funit =
    coll.update
      .one(
        selector,
        $set("response.result" -> Response.Result.cleared, "response.date" -> nowInstant),
        multi = true
      )
      .void

  private def activeFailures(jobType: JobType) =
    $doc("jobType" -> jobType, "response.result" -> Response.Result.failed)

  private def transactionJson(transaction: Transaction): JsObject = Json.obj(
    "jobType" -> transaction.jobType.toString,
    "model" -> transaction.model,
    "source" -> Json
      .obj()
      .add("id" -> transaction.source.id)
      .add("name" -> transaction.source.name)
      .add("url" -> transaction.source.url),
    "updated" -> transaction.updated.toMillis,
    "response" -> transaction.response.map: response =>
      Json.obj(
        "date" -> response.date.toMillis,
        "result" -> response.result.toString,
        "error" -> response.error
      )
  )

object AutomodRepo:
  case class AdminLink(jobType: Automod.JobType, url: String)
