package com.nulabinc.backlog.migration.common.domain.mappings

import com.nulabinc.backlog.migration.common.codec.{
  PriorityMappingDecoder,
  StatusMappingDecoder,
  UserMappingDecoder
}
import org.apache.commons.csv.CSVRecord

object MappingDecoder {

  def status[A, F[_]](records: IndexedSeq[CSVRecord])(implicit
      decoder: StatusMappingDecoder[A]
  ): IndexedSeq[StatusMapping[A]] =
    records.map(decoder.decode)

  def priority[A, F[_]](records: IndexedSeq[CSVRecord])(implicit
      decoder: PriorityMappingDecoder[A]
  ): IndexedSeq[PriorityMapping[A]] =
    records.map(decoder.decode)

  def user[A, F[_]](records: IndexedSeq[CSVRecord])(implicit
      decoder: UserMappingDecoder[A]
  ): IndexedSeq[UserMapping[A]] =
    records.map(decoder.decode)

}
