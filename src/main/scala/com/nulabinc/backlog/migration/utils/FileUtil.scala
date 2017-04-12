package com.nulabinc.backlog.migration.utils

/**
  * @author uchida
  */
object FileUtil {

  def clean(string: String): String = {
    string.replaceAll("\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}", "_")
  }

}
