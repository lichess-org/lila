package lila.video

import play.api.libs.ws.StandaloneWSClient

final private class VideoSheet(ws: StandaloneWSClient, url: String, api: VideoApi)(using Executor):

  import VideoSheet.*

  def fetchAll: Fu[Int] =
    fetch.flatMap: entries =>
      entries
        .sequentially: entry =>
          api.video
            .find(entry.youtubeId)
            .flatMap:
              case Some(video) =>
                val updated = video.copy(
                  title = entry.title,
                  author = entry.author,
                  targets = entry.targets,
                  tags = entry.tags,
                  lang = entry.lang,
                  ads = entry.ads,
                  startTime = entry.startTime
                )
                (video != updated).so:
                  logger.info(s"sheet update $updated")
                  api.video.save(updated)
              case None =>
                val video = Video(
                  _id = entry.youtubeId,
                  title = entry.title,
                  author = entry.author,
                  targets = entry.targets,
                  tags = entry.tags,
                  lang = entry.lang,
                  ads = entry.ads,
                  startTime = entry.startTime,
                  metadata = Youtube.empty,
                  createdAt = nowInstant
                )
                logger.info(s"sheet insert $video")
                api.video.save(video)
            .recover { case e: Exception =>
              logger.warn("sheet update", e)
            }
        .map(_.size)
        .flatMap: processed =>
          api.video
            .removeNotIn(entries.map(_.youtubeId))
            .map: n =>
              if n > 0 then logger.info(s"$n videos removed")
              processed

  private def fetch: Fu[List[Entry]] =
    ws.url(url)
      .get()
      .flatMap:
        case res if res.status == 200 =>
          Future:
            res.bodyAsBytes.utf8String.linesIterator
              .map { line =>
                com.github.tototoshi.csv.CSVParser(
                  input = line,
                  escapeChar = '"',
                  delimiter = ',',
                  quoteChar = '"'
                )
              }
              .flatMap { parsed =>
                for
                  p <- parsed
                  entry <- p match
                    case List(id, author, title, target, tags, lang, ads, _, include, start, _, _) =>
                      val targets = target.split(';').map(_.trim).toList.flatMap(_.toIntOption)
                      Entry(
                        youtubeId = id.trim,
                        author = author.trim,
                        title = title.trim,
                        targets = targets,
                        tags = tags.split(';').map(_.trim.toLowerCase).toList.filter(_.nonEmpty) ::: {
                          if targets contains 1 then List("beginner")
                          else if targets contains 3 then List("advanced")
                          else Nil
                        },
                        lang = lang.trim,
                        include = include.trim == "yes",
                        startTime = ~start.trim.toIntOption,
                        ads = ads.trim == "yes"
                      ).some
                    case _ => none
                  if entry.include
                  if entry.lang == "en"
                yield entry
              }
              .toList
        case res => fufail(s"[video sheet] fetch ${res.status}")

object VideoSheet:

  case class Entry(
      youtubeId: String,
      author: String,
      title: String,
      targets: List[Int],
      tags: List[String],
      lang: String,
      include: Boolean,
      startTime: Int,
      ads: Boolean
  )
