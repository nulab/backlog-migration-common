package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogAttachment

/**
  * @author uchida
  */
trait AttachmentService {

  def postAttachment(path: String): Either[Throwable, BacklogAttachment]

  def allAttachmentsOfIssue(
      issueId: Long
  ): Either[Throwable, Seq[BacklogAttachment]]
}
