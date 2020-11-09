package com.nulabinc.backlog.migration.common.codec

import com.nulabinc.backlog.migration.common.domain.mappings.StatusMapping

trait StatusMappingEncoder[A] extends Encoder[StatusMapping[A], Seq[String]]

trait StatusMappingDecoder[A] extends Decoder[Seq[String], StatusMapping[A]]
