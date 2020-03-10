package com.nulabinc.backlog.migration.common.domain.mappings

import org.apache.commons.csv.CSVRecord

object MappingDeserializer {

  def status[A, F[_]](records: IndexedSeq[CSVRecord])
                     (implicit deserializer: Deserializer[CSVRecord, StatusMapping[A]]): IndexedSeq[StatusMapping[A]] =
    records.map(deserializer.deserialize)

  def priority[A, F[_]](records: IndexedSeq[CSVRecord])
                       (implicit deserializer: Deserializer[CSVRecord, PriorityMapping[A]]): IndexedSeq[PriorityMapping[A]] =
    records.map(deserializer.deserialize)

  def user[A, F[_]](records: IndexedSeq[CSVRecord])
                   (implicit deserializer: Deserializer[CSVRecord, UserMapping[A]]): IndexedSeq[UserMapping[A]] =
    records.map(deserializer.deserialize)

}
