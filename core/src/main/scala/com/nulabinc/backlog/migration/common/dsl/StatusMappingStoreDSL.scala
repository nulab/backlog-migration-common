package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.mappings.StatusMapping
import com.nulabinc.backlog.migration.common.persistence.sqlite.DBIOTypes.{DBIORead, DBIOWrite}

trait StatusMappingQuery[A] {
  def findQuery(mapping: StatusMapping[A]): DBIORead[Option[StatusMapping[A]]]
  def saveQuery(mapping: StatusMapping[A]): DBIOWrite
}

trait StatusMappingStoreDSL[F[_], A] {

  val storeDSL: StoreDSL[F]

  implicit val mq: StatusMappingQuery[A]

  def save(mapping: StatusMapping[A]): F[Int] =
    storeDSL.write(mq.saveQuery(mapping))
}
