package com.nulabinc.backlog.migration.importer.service

import java.util.Date

import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, DateUtil, Logging, ProgressBar}
import com.osinka.i18n.Messages
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi.ansi

/**
 * @author
 *   uchida
 */
private[importer] class IssueProgressBar() extends Logging {

  var totalSize = 0
  var count     = 0
  var failed    = 0
  var date      = ""

  private[this] var newLine       = false
  private[this] var isMessageMode = false
  private[this] val timer         = (timerFunc _)()

  private[this] def timerFunc() = {
    var tempTime: Long         = System.currentTimeMillis()
    var totalElapsedTime: Long = 0
    () => {
      val elapsedTime: Long = System.currentTimeMillis() - tempTime
      totalElapsedTime = totalElapsedTime + elapsedTime
      val average: Float = totalElapsedTime.toFloat / count.toFloat
      tempTime = System.currentTimeMillis()
      val remaining           = totalSize - count
      val remainingTime: Long = (remaining * average).toLong

      DateUtil.timeFormat(new Date(remainingTime))
    }
  }

  def warning(indexOfDate: Int, totalOfDate: Int, value: String) = {
    message(indexOfDate: Int, totalOfDate: Int, value: String, YELLOW)
  }

  def error(indexOfDate: Int, totalOfDate: Int, value: String) = {
    message(indexOfDate: Int, totalOfDate: Int, value: String, RED)
  }

  private[this] def message(
      indexOfDate: Int,
      totalOfDate: Int,
      value: String,
      color: Ansi.Color
  ) = {
    clear()
    val message =
      s"""${(" " * 11) + ansi().fg(color).a(value.replaceAll("\n", "")).reset().toString}
         |${current(indexOfDate, totalOfDate)}
         |--------------------------------------------------
         |${remaining()}""".stripMargin

    ConsoleOut.outStream.println(message)
    isMessageMode = true
  }

  def progress(indexOfDate: Int, totalOfDate: Int) = {
    newLine = indexOfDate == 1
    clear()
    val message =
      s"""${current(indexOfDate, totalOfDate)}
         |--------------------------------------------------
         |${remaining()}""".stripMargin
    ConsoleOut.outStream.println(message)
    isMessageMode = false
  }

  private[this] def clear() = {
    if (newLine && !isMessageMode) {
      ConsoleOut.outStream.println()
    }
    (0 until 3).foreach { _ =>
      ConsoleOut.outStream.print(
        ansi.cursorLeft(999).cursorUp(1).eraseLine(Ansi.Erase.ALL)
      )
    }
    ConsoleOut.outStream.flush()
    newLine = false
  }

  private[this] def current(indexOfDate: Int, totalOfDate: Int): String = {
    val progressBar = ProgressBar.progressBar(indexOfDate, totalOfDate)
    val resultString =
      if (failed == 0) Messages("common.result_success")
      else Messages("common.result_failed", failed)
    val result = if (resultString.nonEmpty) {
      if (resultString == Messages("common.result_success"))
        s"[${ansi().fg(GREEN).a(resultString).reset()}]"
      else s"[${ansi().fg(RED).a(resultString).reset()}]"
    } else resultString

    val message =
      Messages(
        "import.date.execute",
        date,
        Messages("common.issues"),
        if (indexOfDate == totalOfDate) Messages("message.imported")
        else Messages("message.importing")
      )

    s"${progressBar}${result} ${message}"
  }

  private[this] def remaining(): String = {
    val progressBar = ProgressBar.progressBar(count + 1, totalSize)
    val message     = Messages("import.progress", count + 1, totalSize)
    val time        = Messages("import.remaining_time", timer())
    s"${progressBar} ${message}${time}"
  }

}
