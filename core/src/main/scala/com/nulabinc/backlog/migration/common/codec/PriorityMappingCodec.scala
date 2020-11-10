package com.nulabinc.backlog.migration.common.codec

import com.nulabinc.backlog.migration.common.domain.mappings.PriorityMapping
import org.apache.commons.csv.CSVRecord

trait PriorityMappingEncoder[A] extends Encoder[PriorityMapping[A], Seq[String]]

trait PriorityMappingDecoder[A] extends Decoder[CSVRecord, PriorityMapping[A]]
