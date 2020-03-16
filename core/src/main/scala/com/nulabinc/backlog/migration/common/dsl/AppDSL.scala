package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.shared.Result.Result
import simulacrum.typeclass

@typeclass
trait AppDSL[F[_]] {
  def pure[A](a: A): F[A]
  def fromError[E, A](error: E): F[Result[E, A]]
}
