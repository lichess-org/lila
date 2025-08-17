/*
 * https://github.com/akka/akka-stream-contrib/blob/main/src/main/scala/akka/stream/contrib/ZipInputStreamSource.scala
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.contrib

import akka.stream.Attributes.{ InputBuffer, name }
import akka.stream.contrib.ZipInputStreamSource.ZipEntryData
import akka.stream.impl.Stages.DefaultAttributes.IODispatcher
import akka.stream.scaladsl.Source
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, OutHandler }
import akka.stream.{ Attributes, Outlet, SourceShape }
import akka.util.ByteString
import akka.util.ByteString.ByteString1C

import java.util.zip.{ ZipEntry, ZipInputStream }
import scala.annotation.tailrec
import scala.collection.immutable
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

/** This companion defines a factory for [[ZipInputStreamSource]] instances, see
  * [[ZipInputStreamSource.apply]].
  */
object ZipInputStreamSource:

  final val DefaultChunkSize = 8192
  final val DefaultAllowedZipExtensions = immutable.Seq(".zip")

  /** Data type for zip entries.
    *
    * @param name
    *   file name
    * @param creationTime
    *   The last modification time of the entry in milliseconds since the epoch, or -1 if not specified
    */
  final case class ZipEntryData(name: String, creationTime: Long)

  /** Factory for [[ZipInputStreamSource]] instances wrapped into [[Source]].
    *
    * @param in
    *   a function that builds a [[ZipInputStream]]
    * @param chunkSize
    *   the size of the chunks
    * @param allowedZipExtensions
    *   collection of allowed extensions for zipped containers, as zip or jar files. Only ".zip" extension
    *   allowed by default.
    * @return
    *   [[ZipInputStreamSource]] instance
    */
  def apply(
      in: () => ZipInputStream,
      chunkSize: Int = DefaultChunkSize,
      allowedZipExtensions: immutable.Seq[String] = DefaultAllowedZipExtensions
  ): Source[(ZipEntryData, ByteString), Future[Long]] =
    Source
      .fromGraph(new ZipInputStreamSource(in, chunkSize, allowedZipExtensions))
      .withAttributes(name("zipInputStreamSource").and(IODispatcher))

  /** Java API: Factory for [[ZipInputStreamSource]] instances wrapped into [[Source]].
    *
    * @param in
    *   a function that builds a [[ZipInputStream]]
    * @param chunkSize
    *   the size of the chunks
    * @param allowedZipExtensions
    *   collection of allowed extensions for zipped containers, as zip or jar files. Only ".zip" extension
    *   allowed by default.
    * @return
    *   [[ZipInputStreamSource]] instance
    */
  def create(
      in: Function0[ZipInputStream],
      chunkSize: Int = DefaultChunkSize,
      allowedZipExtensions: immutable.Seq[String] = DefaultAllowedZipExtensions
  ): Source[(ZipEntryData, ByteString), Future[Long]] =
    Source
      .fromGraph(new ZipInputStreamSource(in, chunkSize, allowedZipExtensions))
      .withAttributes(name("zipInputStreamSource").and(IODispatcher))

/** A stage that works as a [[Source]] of data chunks extracted from zip files. In addition to regular files,
  * the zip file might contain directories and other zip files. Every chunk is a tuple of [[ZipEntryData]] and
  * [[ByteString]], where the former carries basic info about the file from which the bytes come and the
  * latter carries those bytes. This stage materializes to the total amount of read bytes.
  *
  * @param in
  *   a function that builds a [[ZipInputStream]]
  * @param chunkSize
  *   the size of the chunks
  */
final class ZipInputStreamSource private (
    in: () => ZipInputStream,
    chunkSize: Int,
    allowedZipExtensions: immutable.Seq[String]
) extends GraphStageWithMaterializedValue[SourceShape[(ZipEntryData, ByteString)], Future[Long]]:

  val matValue = Promise[Long]()

  override val shape =
    SourceShape(Outlet[(ZipEntryData, ByteString)]("zipInputStreamSource.out"))

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) =

    val InputBuffer(initialBuffer, maxBuffer) =
      inheritedAttributes.getAttribute(classOf[InputBuffer], InputBuffer(16, 16))

    val logic = new GraphStageLogic(shape):
      import shape.*

      private var is: ZipInputStream = null
      private var readBytesTotal: Long = 0L
      private var buffer: Vector[(ZipEntryData, ByteString)] = Vector.empty
      private var eof: Boolean = false
      private var currentEntry: Option[ZipEntry] = None
      private var currentStreams: Seq[ZipInputStream] = Seq()

      override def preStart(): Unit =
        super.preStart()
        try
          is = in()
          currentStreams = List(is)
          fillBuffer(initialBuffer)
        catch
          case NonFatal(ex) =>
            matValue.failure(ex)
            failStage(ex)

      override def postStop(): Unit =
        if is != null then is.close()
        super.postStop()

      setHandler(
        out,
        new OutHandler:

          override def onPull(): Unit =
            fillBuffer(maxBuffer)
            buffer match
              case head +: Seq() =>
                push(out, head)
                finalize()
              case head +: tail =>
                push(out, head)
                buffer = tail
              case _ => finalize()
            def finalize() =
              try is.close()
              finally
                matValue.success(readBytesTotal)
                complete(out)

          override def onDownstreamFinish(cause: Throwable): Unit =
            try is.close()
            finally
              matValue.success(readBytesTotal)
              super.onDownstreamFinish(cause)
      ) // end of handler

      @tailrec private def nextEntry(streams: Seq[ZipInputStream]): (Option[ZipEntry], Seq[ZipInputStream]) =
        streams match
          case Seq() => (None, streams)
          case (z :: zs) =>
            val entry = Option(z.getNextEntry)
            entry match
              case None =>
                nextEntry(zs)
              case Some(e) if isZipFile(e) =>
                nextEntry(new ZipInputStream(z) +: streams)
              case Some(e) if e.isDirectory =>
                nextEntry(streams)
              case _ =>
                (entry, streams)

      private def isZipFile(e: ZipEntry) =
        val name = e.getName.toLowerCase
        allowedZipExtensions.exists(name.endsWith)

      /** BLOCKING I/O READ */
      private def readChunk() =
        def read(arr: Array[Byte]) = currentStreams.headOption.flatMap { stream =>
          val readBytes = stream.read(arr)
          if readBytes == -1 then
            val (entry, streams) = nextEntry(currentStreams)
            currentStreams = streams
            currentEntry = entry
            entry.map(zipEntry => (zipEntry, streams.head.read(arr)))
          else Some((currentEntry.get, readBytes))
        }
        val arr = Array.ofDim[Byte](chunkSize)
        read(arr) match
          case None =>
            eof = true
          case Some(entry, readBytes) =>
            readBytesTotal += readBytes
            val entryData = ZipEntryData(entry.getName, entry.getTime)
            val chunk =
              if readBytes == chunkSize then ByteString1C(arr)
              else ByteString1C(arr).take(readBytes)
            buffer :+= ((entryData, chunk))
      // readChunk

      private def fillBuffer(size: Int) =
        while buffer.length < size && !eof do readChunk()

    (logic, matValue.future)
