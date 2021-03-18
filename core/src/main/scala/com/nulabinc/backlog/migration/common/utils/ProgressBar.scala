package com.nulabinc.backlog.migration.common.utils

import ConsoleOut.outStream
import com.osinka.i18n.Messages
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.ansi
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import monix.eval.Task
import monix.execution.Scheduler

/**
 * @author uchida
 */
object ProgressBar extends Logging {

  def progress(
      name: String,
      progressMessage: String,
      completeMessage: String
  )(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler) = {
    var initFlag = true
    (index: Int, total: Int) => {
      val message =
        progressValue(name, progressMessage, completeMessage, index, total)
      logger.info(message)
      synchronized {
        if (initFlag) {
          outStream.println()
          initFlag = false
        }
        outStream.print(
          ansi.cursorLeft(999).cursorUp(1).eraseLine(Ansi.Erase.ALL)
        )
        outStream.flush()
        outStream.println(
          s" ${progressBar(index, total)}${ConsoleDSL[Task].bold(message).runSyncUnsafe()}"
        )
      }
    }
  }

  private[this] def progressValue(
      name: String,
      progressMessage: String,
      completeMessage: String,
      index: Int,
      total: Int
  ) = {
    if (index == total)
      Messages("message.progress.executed", completeMessage, name)
    else Messages("message.progress.executed", progressMessage, name)
  }

  def progressBar(index: Int, total: Int): String = {
    val decile   = (10.0 * (index.toFloat / total.toFloat)).toInt
    val rate     = index.toFloat / total.toFloat
    val progress = Messages("message.progress.value", index, total)
    val value =
      s"${progress} [${("#" * decile)}${(" " * (10 - decile))}] " + f"${100.0 * rate}%5.1f%% "
    padLeft(value, 30)
  }

  private[this] def padLeft(value: String, length: Int): String = {
    if (value.length < length) {
      (" " * (length - value.length)) + value
    } else value
  }

}
