package com.nulabinc.backlog.migration.common.codec

import com.nulabinc.backlog.migration.common.domain.mappings.UserMapping

trait UserMappingEncoder[A] extends Encoder[UserMapping[A], Seq[String]]

trait UserMappingDecoder[A] extends Decoder[Seq[String], UserMapping[A]]
