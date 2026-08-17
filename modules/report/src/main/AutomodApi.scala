package lila.report

import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*

import lila.core.data.Text
import lila.core.id.ImageId
import lila.mon.extensions.*
import lila.memo.{ Dimensions, ImageAutomod, ImageAutomodRequest }
import lila.memo.SettingStore.Text.given

final class AutomodApi(
    ws: StandaloneWSClient,
    appConfig: Configuration,
    settingStore: lila.memo.SettingStore.Builder,
    picfitApi: lila.memo.PicfitApi,
    repo: AutomodRepo
)(using Executor):

  private case class ParsedResponse(data: JsObject, usage: Option[Automod.Usage])

  private val config = appConfig.get[Automod.Config]("automod")
  private val healthState = AtomicReference(Automod.Health())

  def health: Automod.Status = healthState.get.status

  def failed(jobType: Automod.JobType, offset: Int, limit: Int): Fu[List[Automod.Transaction]] =
    repo.failed(jobType, offset, limit)

  def failedCount(jobType: Automod.JobType): Fu[Int] = repo.failedCount(jobType)

  def failedSourceIds(jobType: Automod.JobType): Fu[List[String]] = repo.failedSourceIds(jobType)

  def clear(jobType: Automod.JobType, sourceId: String): Funit = repo.clear(jobType, sourceId)

  def clearAll(jobType: Automod.JobType): Funit = repo.clearAll(jobType)

  val imagePromptSetting = settingStore[Text](
    "imageAutomodPrompt",
    text = "Image automod prompt".some,
    default = Text("")
  )

  val imageModelSetting = settingStore[String](
    "imageAutomodModel",
    text = "Image automod model".some,
    default = "Qwen/Qwen3.5-9B"
  )

  lila.common.Bus.sub[ImageAutomodRequest]: req =>
    imageFlagReason(req.id, req.dim.some)
      .map: flagged =>
        picfitApi.setAutomod(req.id, ImageAutomod(flagged))

  def apply(request: Automod.Request): Fu[JsObject] =
    List(config.apiKey.value, request.prompt.value, request.input.hashInput)
      .forall(_.nonEmpty)
      .so:
        track(request)(
          request.timeout
            .fold(
              ws.url(config.url)
                .withHttpHeaders(
                  "Authorization" -> s"Bearer ${config.apiKey.value}",
                  "Content-Type" -> "application/json"
                )
            ):
              ws.url(config.url)
                .withHttpHeaders(
                  "Authorization" -> s"Bearer ${config.apiKey.value}",
                  "Content-Type" -> "application/json"
                )
                .withRequestTimeout(_)
            .post(requestBody(request))
            .flatMap: rsp =>
              extractJsonFromResponse(rsp)
                .toTry(s"Automod ${request.model} invalid response: ${rsp.body[String].takeRight(200)}")
                .toFuture
        ).map(_.data)

  def markdownImages(markdown: Markdown): Fu[Seq[lila.memo.PicfitImage]] =
    val ids = picfitApi.imageIds(markdown)
    picfitApi
      .byIds(ids)
      .flatMap:
        _.map: pic =>
          if pic.automod.isDefined then fuccess(pic)
          else
            for
              flagged <- imageFlagReason(pic.id, pic.dimensions)
              automod = ImageAutomod(flagged)
              _ <- picfitApi.setAutomod(pic.id, automod)
            yield pic.copy(automod = automod.some)
        .toSeq.parallel

  private def imageFlagReason(id: ImageId, dim: Option[Dimensions]): Fu[Option[String]] =
    val (apiKey, model, prompt) =
      (config.apiKey.value, imageModelSetting.get(), imagePromptSetting.get().value)
    List(apiKey, model, prompt)
      .forall(_.nonEmpty)
      .so:
        val imageUrl = picfitApi.url.automod(id, dim)
        ws.url(imageUrl.value)
          .get()
          .flatMap: imageRsp =>
            if imageRsp.status != 200 then
              fufail(s"Picfit image $id returned ${imageRsp.status} ${imageRsp.statusText}")
            else
              val contentType = if id.value.endsWith(".png") then "image/png" else "image/webp"
              val encoded = Base64.getEncoder.encodeToString(imageRsp.bodyAsBytes.toArray)
              apply(
                Automod.Request.image(
                  job = Automod.Job(Automod.JobType.image, Automod.Source(id = id.value.some)),
                  imageUrl = s"data:$contentType;base64,$encoded",
                  prompt = Text(prompt),
                  model = model
                )
              )
          .prefixFailure(s"Automod image $id request failed")
          .map: res =>
            val flagged = ~res.boolean("flag")
            lila.mon.mod.report.automod.imageFlagged(flagged).increment()
            flagged.option:
              res.str("reason") | "No reason provided"
          .monSuccess(lila.mon.mod.report.automod.imageRequest)
          .recover:
            case err =>
              logger.error(err.getMessage, err)
              none

  private def extractJsonFromResponse(rsp: StandaloneWSResponse): Option[ParsedResponse] =
    if rsp.status != 200 then none
    else
      scala.util
        .Try:
          val body = rsp.body[String]
          val streamed = body.linesIterator
            .collect:
              case s"data:$event" if event.trim != "[DONE]" =>
                Json.parse(event)
            .toList
          val response = streamed.headOption.getOrElse(Json.parse(body))
          val content = streamed.flatMap: event =>
            (event \ "choices")
              .as[List[JsObject]]
              .flatMap(choice => (choice \ "delta" \ "content").asOpt[String])
          val msg = if streamed.nonEmpty then content.mkString
          else ((response \ "choices").as[List[JsObject]].head \ "message" \ "content").as[String]
          val usage = (streamed :+ response).reverseIterator.collectFirst:
            case event if (event \ "usage").asOpt[JsObject].isDefined =>
              val tokens = (event \ "usage").as[JsObject]
              Automod.Usage(
                (tokens \ "input_tokens").asOpt[Int].orElse((tokens \ "prompt_tokens").asOpt[Int]).get,
                (tokens \ "output_tokens").asOpt[Int].orElse((tokens \ "completion_tokens").asOpt[Int]).get
              )
          ParsedResponse(
            Json
              .parse(msg.slice(msg.indexOf('{', msg.indexOf("</think>")), msg.lastIndexOf('}') + 1))
              .as[JsObject],
            usage
          )
        .toOption

  private def requestBody(request: Automod.Request): JsObject =
    val messages = request.input match
      case Automod.Input.Text(userText) =>
        Json.arr(
          Json.obj("role" -> "system", "content" -> request.prompt.value),
          Json.obj("role" -> "user", "content" -> userText)
        )
      case Automod.Input.Image(imageUrl) =>
        Json.arr(
          Json.obj(
            "role" -> "user",
            "content" -> Json.arr(
              Json.obj("type" -> "text", "text" -> request.prompt.value),
              Json.obj("type" -> "image_url", "image_url" -> Json.obj("url" -> imageUrl))
            )
          )
        )
    Json.obj(
      "model" -> request.model,
      "temperature" -> request.temperature,
      "max_tokens" -> 4096,
      "messages" -> messages,
      "stream" -> true,
      "stream_options" -> Json.obj("include_usage" -> true),
      "reasoning" -> Json.obj("enabled" -> request.reasoning),
      "enable_thinking" -> request.reasoning,
      "response_format" -> Json.obj("type" -> "json_object")
    )

  private def track(request: Automod.Request)(f: Fu[ParsedResponse]): Fu[ParsedResponse] =
    repo
      .create(request)
      .flatMap: transaction =>
        f.transformWith:
          case scala.util.Success(rsp) =>
            repo
              .respond(transaction, Automod.Response.success(nowInstant, Json.stringify(rsp.data), rsp.usage))
              .inject(rsp)
          case scala.util.Failure(error) =>
            val message =
              Option(error.getMessage).filter(_.nonEmpty).map(_.take(500)) | error.getClass.getSimpleName
            repo
              .respond(transaction, Automod.Response.failed(nowInstant, message))
              .flatMap(_ => fufail(error))
      .andThen:
        case scala.util.Success(_) => healthState.getAndUpdate(_.record(success = true))
        case scala.util.Failure(error) =>
          healthState.getAndUpdate(_.record(success = false))
          logger.warn(s"Automod ${request.model} failed", error)
