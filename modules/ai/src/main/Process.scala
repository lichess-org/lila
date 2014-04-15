package lila.ai

import java.io.OutputStream
import scala.io.Source.fromInputStream
import scala.sys.process.{ Process => SProcess, ProcessBuilder, ProcessIO }

private[ai] final class Process(
    builder: ProcessBuilder,
    name: String,
    out: String => Unit,
    err: String => Unit,
    debug: Boolean) {

  doLog("start process")

  def write(msg: String) {
    log("> " + msg)
    in write (msg + "\n").getBytes("UTF-8")
    in.flush()
  }

  def destroy() {
    doLog("destroy process")
    try {
      write("stop")
      write("quit")
      Thread sleep 200
    }
    catch {
      case e: java.io.IOException => logwarn(s"[$name] process destroy " + e.getMessage)
    }
    process.destroy()
  }

  private var in: OutputStream = _
  private val processIO = new ProcessIO(
    i => {
      in = i
    },
    o => fromInputStream(o).getLines foreach { line =>
      log(line)
      out(line)
    },
    e => fromInputStream(e).getLines foreach { line =>
      log(line)
      err(line)
    })
  private val process = builder run processIO

  private def log(msg: String) {
    if (debug) doLog(msg)
  }

  private def doLog(msg: String) {
    loginfo(s"[$name] $msg")
  }
}

object Process {

  def apply(execPath: String, name: String)(out: String => Unit, err: String => Unit, debug: Boolean) =
    new Process(
      builder = SProcess(execPath),
      name = name,
      out = out,
      err = err,
      debug = debug)

  type Builder = (String => Unit, String => Unit, Boolean) => Process
}

