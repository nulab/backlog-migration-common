package com.nulabinc.backlog.migration.common.interpreters

import java.io.PrintStream

import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.utils.ConsoleOut.bold
import com.nulabinc.backlog.migration.common.utils.Logging
import monix.eval.Task
import org.fusesource.jansi.Ansi

class JansiConsoleDSL extends ConsoleDSL[Task] with Logging {
  private val outStream: PrintStream = System.out

  override def boldln(value: String, space: Int, color: Ansi.Color): Task[Unit] = Task.eval {
    logger.info(value)
    outStream.println((" " * space) + bold(value, color))
    outStream.flush()
  }
}
