package com.nulabinc.backlog.migration.common.utils

import java.util.Locale

import com.osinka.i18n.Lang
import org.slf4j.{Logger, LoggerFactory}

/**
 * @author
 *   uchida
 */
trait Logging {

  implicit lazy val userLang =
    if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val logger: Logger = LoggerFactory.getLogger(getClass)

}
