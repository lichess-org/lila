package lila
package ai.stockfish

import java.io.OutputStream
import scala.sys.process.{ Process ⇒ SProcess, ProcessBuilder, ProcessIO }
import scala.io.Source.fromInputStream

final class Process(
    builder: ProcessBuilder,
    name: String,
    out: String ⇒ Unit,
    err: String ⇒ Unit,
    debug: Process.Debug) {

  def write(msg: String) {
    log(msg)
    in write (msg + "\n").getBytes("UTF-8")
    in.flush()
  }

  def destroy() {
    try {
      write("stop")
      write("quit")
      Thread sleep 300
    }
    catch {
      case e: java.io.IOException ⇒ log(e.getMessage)
    }
    process.destroy()
  }

  private var in: OutputStream = _
  private val processIO = new ProcessIO(
    i ⇒ {
      in = i
    },
    o ⇒ fromInputStream(o).getLines foreach { line ⇒
      log(line)
      out(line)
    },
    e ⇒ fromInputStream(e).getLines foreach { line ⇒
      log(line)
      err(line)
    })
  private val process = builder run processIO

  private def log(msg: String) {
    if (debug(msg)) println("[%s] %s".format(name, msg))
  }
}

object Process {

  def apply(execPath: String, name: String)(out: String ⇒ Unit, err: String ⇒ Unit, debug: Debug) =
    new Process(
      builder = SProcess(execPath),
      name = name,
      out = out,
      err = err,
      debug = debug)

  type Debug = String ⇒ Boolean

  val DEBUG_ALL: Debug = _ ⇒ true
  val DEBUG_NONE: Debug = _ ⇒ false

  type Builder = (String ⇒ Unit, String ⇒ Unit, Debug) ⇒ Process
}

