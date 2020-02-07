package com.nulabinc.backlog.migration.common.dsl

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color.BLACK

trait ConsoleDSL[F[_]] {

  def boldln(value: String, space: Int = 0, color: Ansi.Color = BLACK): F[Unit]
}
