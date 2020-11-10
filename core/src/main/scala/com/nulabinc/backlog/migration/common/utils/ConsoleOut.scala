package com.nulabinc.backlog.migration.common.utils

import java.io.PrintStream

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi.ansi

/**
 * @author uchida
 */
object ConsoleOut extends Logging {

  val outStream: PrintStream = System.out

  @deprecated("ConsoleDSL[F].errorln")
  def error(value: String, space: Int = 0): Unit =
    println(value, space, RED)

  def success(value: String, space: Int = 0): Unit =
    println(value, space, GREEN)

  def warning(value: String, space: Int = 0): Unit =
    println(value, space, YELLOW)

  @deprecated("ConsoleDSL[F].infoln")
  def info(value: String, space: Int = 0): Unit =
    println(value, space, BLUE)

  @deprecated("ConsoleDSL[F].println")
  def println(
      value: String,
      space: Int = 0,
      color: Ansi.Color = BLACK
  ): PrintStream = {
    logger.info(value)
    if (color == BLACK) {
      outStream.println((" " * space) + ansi().a(value).reset().toString)
    } else {
      outStream.println(
        (" " * space) + ansi().fg(color).a(value).reset().toString
      )
    }

    outStream.flush()
    outStream
  }

  def bold(value: String, color: Ansi.Color = BLACK): String = {
    if (color == BLACK) {
      ansi().bold().a(value).reset().toString
    } else {
      ansi().fg(color).bold().a(value).reset().toString
    }
  }

  @deprecated("ConsoleDSL[F].boldln")
  def boldln(value: String, space: Int = 0, color: Ansi.Color = BLACK) = {
    logger.info(value)
    outStream.println((" " * space) + bold(value, color))
    outStream.flush()
    outStream
  }

  def overwrite(value: String, space: Int = 0) = {
    logger.info(value)

    synchronized {
      outStream.print(
        ansi.cursorLeft(999).cursorUp(1).eraseLine(Ansi.Erase.ALL)
      )
      outStream.flush()
      outStream.println((" " * space) + value)
    }
  }

}
