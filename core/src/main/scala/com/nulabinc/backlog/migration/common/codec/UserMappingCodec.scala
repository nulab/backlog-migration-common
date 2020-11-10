package com.nulabinc.backlog.migration.common.codec

import com.nulabinc.backlog.migration.common.domain.mappings.UserMapping
import org.apache.commons.csv.CSVRecord

trait UserMappingEncoder[A] extends Encoder[UserMapping[A], Seq[String]]

trait UserMappingDecoder[A] extends Decoder[CSVRecord, UserMapping[A]]
