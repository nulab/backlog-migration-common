package com.nulabinc.backlog.migration.common.codec

case class Codec[A, B](encoder: Encoder[A, B], decoder: Decoder[A, B])
