package com.nulabinc.backlog.migration.common.interpreters

import java.io.PrintStream

import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.utils.Logging
import monix.eval.Task
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi.ansi

case class JansiConsoleDSL() extends ConsoleDSL[Task] with Logging {
  private val outStream: PrintStream = System.out

  override def infoln(value: String, space: Int = 0): Task[Unit] =
    println(value, space, BLUE)

  override def println(
      value: String,
      space: Int,
      color: Ansi.Color
  ): Task[Unit] =
    Task.eval {
      logger.info(value)
      if (color == BLACK) {
        outStream.println((" " * space) + ansi().a(value).reset().toString)
      } else {
        outStream.println(
          (" " * space) + ansi().fg(color).a(value).reset().toString
        )
      }

      outStream.flush()
    }

  override def printStream(value: Ansi): Task[Unit] =
    Task(outStream.print(value))

  override def bold(value: String, color: Ansi.Color = BLACK): Task[String] =
    if (color == BLACK) {
      Task(ansi().bold().a(value).reset().toString)
    } else {
      Task(ansi().fg(color).bold().a(value).reset().toString)
    }

  override def boldln(
      value: String,
      space: Int,
      color: Ansi.Color
  ): Task[Unit] =
    Task.eval {
      logger.info(value)
      outStream.println((" " * space) + bold(value, color))
      outStream.flush()
    }

  override def errorln(value: String, space: Int): Task[Unit] =
    println(value, space, RED)

  override def warnln(value: String, space: Int = 0): Task[Unit] =
    println(value, space, YELLOW)

  override def read(message: String): Task[String] =
    Task.eval {
      scala.io.StdIn.readLine(message)
    }

  override def flush(): Task[Unit] =
    Task(outStream.flush())

  override def success(value: String, space: Int = 0): Task[Unit] =
    Task(println(value, space, GREEN))

  override def warning(value: String, space: Int = 0): Task[Unit] =
    println(value, space, YELLOW)

  override def overwrite(value: String, space: Int = 0): Task[Unit] = {
    logger.info(value)

    Task {
      synchronized {
        outStream.print(
          ansi.cursorLeft(999).cursorUp(1).eraseLine(Ansi.Erase.ALL)
        )
        outStream.flush()
        outStream.println((" " * space) + value)
      }
    }
  }
}
