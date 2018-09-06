package com.nulabinc.backlog.migration.common.utils
import java.net.HttpURLConnection

import scala.util.control.Exception.ignoring

object ControlUtil {

  def defining[A, B](value: A)(f: A => B): B = f(value)

  def using[A <% { def close(): Unit }, B](resource: A)(f: A => B): B =
    try f(resource)
    finally {
      if (resource != null) {
        ignoring(classOf[Throwable]) {
          resource.close()
        }
      }
    }

  def using[T](connection: HttpURLConnection)(f: HttpURLConnection => T): T =
    try f(connection)
    finally connection.disconnect()

  def ignore[T](f: => Unit): Unit =
    try {
      f
    } catch {
      case _: Exception => ()
    }
}
