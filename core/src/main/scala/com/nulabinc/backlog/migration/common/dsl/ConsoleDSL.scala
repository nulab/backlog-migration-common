package com.nulabinc.backlog.migration.common.dsl

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color.BLACK
import simulacrum.typeclass

@typeclass
trait ConsoleDSL[F[_]] {
  def infoln(value: String, space: Int = 0): F[Unit]
  def println(value: String, space: Int = 0, color: Ansi.Color = BLACK): F[Unit]
  def printStream(value: Ansi): F[Unit]
  def bold(value: String, color: Ansi.Color = BLACK): F[String]
  def boldln(value: String, space: Int = 0, color: Ansi.Color = BLACK): F[Unit]
  def errorln(value: String, space: Int = 0): F[Unit]
  def warnln(value: String, space: Int = 0): F[Unit]
  def read(message: String): F[String]
  def flush(): F[Unit]
  def success(value: String, space: Int = 0): F[Unit]
  def warning(value: String, space: Int = 0): F[Unit]
  def overwrite(value: String, space: Int = 0): F[Unit]
}
