package com.nulabinc.backlog.migration.utils

import java.util.Locale

import com.nulabinc.backlog4j.BacklogAPIException
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
object MessageUtil {

  implicit val userLang =
    if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  private[this] val MESSAGE_EXTRACT_BEGIN = "message - : "
  private[this] val MESSAGE_EXTRACT_END   = "code"

  def getMessage(value: String) = {
    val subsequentMessage = getSubsequentMessage(value)
    cutAfterCode(subsequentMessage)
  }

  private[this] def getSubsequentMessage(value: String) = {
    val messagePos: Int = value.indexOf(MESSAGE_EXTRACT_BEGIN)
    if (messagePos >= 0)
      value.substring(messagePos + MESSAGE_EXTRACT_BEGIN.length)
    else value
  }

  private[this] def cutAfterCode(value: String) = {
    val codePos: Int = value.indexOf(MESSAGE_EXTRACT_END)
    if (codePos >= 0) value.substring(0, codePos)
    else value
  }

  def apply(key: String): String = {
    Messages(key)(userLang)
  }

  def error(e: Throwable): String = e match {
    case bae: BacklogAPIException => bae.getMessage
    case _: Throwable             => Messages("cli.error.unknown")(userLang)
  }

}
