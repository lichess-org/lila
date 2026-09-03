package lila.report

import com.roundeights.hasher.Algo
import play.api.ConfigLoader
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.common.autoconfig.AutoConfig
import lila.common.config.given
import lila.core.config.Secret
import lila.core.data.Text
object Automod:
  enum JobType:
    case blog, comms, image

  case class Source(id: Option[String] = none, name: Option[String] = none, url: Option[String] = none)

  case class Job(
      jobType: JobType,
      source: Source,
      config: String = "",
      hash: Option[String] = none
  )

  enum Input:
    case Text(value: String)
    case Image(url: String)

    def hashInput: String = this match
      case Text(value) => value
      case Image(url) => url

  case class Request(
      job: Job,
      prompt: Text,
      model: String,
      input: Input,
      temperature: Double = 0,
      reasoning: Boolean = false,
      timeout: Option[scala.concurrent.duration.FiniteDuration] = none
  ):
    def hash: String = job.hash.getOrElse(defaultHash(job, input.hashInput))

  object Request:
    def text(
        job: Job,
        userText: String,
        prompt: Text,
        model: String,
        temperature: Double = 0,
        reasoning: Boolean = false
    ) = Request(job, prompt, model, Input.Text(userText), temperature, reasoning)

    def image(job: Job, imageUrl: String, prompt: Text, model: String) =
      Request(job, prompt, model, Input.Image(imageUrl), timeout = 10.minutes.some)

  extension (request: Request) def id: String = jobId(request.job, request.input.hashInput)

  case class Usage(
      inputTokens: Int,
      outputTokens: Int
  ):
    def totalTokens: Int = inputTokens + outputTokens

  case class Response(
      date: Instant,
      result: Response.Result,
      output: Option[String] = none,
      error: Option[String] = none,
      usage: Option[Usage] = none
  )

  object Response:
    enum Result:
      case success, failed, cleared

    def success(date: Instant, output: String, usage: Option[Usage]) =
      Response(date, Result.success, output.some, usage = usage)
    def failed(date: Instant, error: String) = Response(date, Result.failed, error = error.some)

  case class Transaction(
      @Key("_id") id: String,
      jobType: JobType,
      config: String,
      source: Source,
      model: String,
      updated: Instant,
      response: Option[Response] = none,
      failures: Int = 0
  )

  def defaultHash(job: Job, input: String): String =
    Algo.sha256(s"${job.jobType}:${job.config}:$input").hex.take(16)

  def jobId(job: Job, input: String): String =
    s"${job.jobType}:${job.hash.getOrElse(defaultHash(job, input))}"

  enum Status:
    case green, yellow, red

  case class Health(recent: List[Boolean] = Nil):
    def record(success: Boolean): Health = copy(recent = (success :: recent).take(10))

    def status: Status =
      if recent.size > 0 && recent.take(5).forall(!_) then Status.red
      else if recent.contains(false) then Status.yellow
      else Status.green

  case class Config(val url: String, val apiKey: Secret)
  given ConfigLoader[Config] = AutoConfig.loader[Config]
