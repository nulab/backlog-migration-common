package com.nulabinc.backlog.migration.common.domain

import shapeless.tag.@@

trait Source
trait Destination

object IssueTags {
  type SourceIssue = BacklogIssue @@ Source
  type DestinationIssue = BacklogIssue @@ Destination
}
