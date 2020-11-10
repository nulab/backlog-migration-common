package com.nulabinc.backlog.migration.common.codec

import com.nulabinc.backlog.migration.common.domain.mappings.StatusMapping
import org.apache.commons.csv.CSVRecord

trait StatusMappingEncoder[A] extends Encoder[StatusMapping[A], Seq[String]]

trait StatusMappingDecoder[A] extends Decoder[CSVRecord, StatusMapping[A]]
