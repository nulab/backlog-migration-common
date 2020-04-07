package com.nulabinc.backlog.migration.common.dsl

import simulacrum.typeclass

@typeclass
trait AppDSL[F[_]] {
  def pure[A](a: A): F[A]
  def fromError[E, A](error: E): F[Either[E, A]]
}
