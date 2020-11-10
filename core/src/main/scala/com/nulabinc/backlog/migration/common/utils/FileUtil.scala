package com.nulabinc.backlog.migration.common.utils

import java.text.Normalizer

/**
 * @author uchida
 */
object FileUtil {

  def clean(string: String): String = {
    normalize(string).replaceAll("\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}", "_")
  }

  def normalize(string: String): String = {
    Normalizer.normalize(Option(string).getOrElse(""), Normalizer.Form.NFC)
  }

}
