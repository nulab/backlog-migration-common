package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogAttachment

/**
  * @author uchida
  */
trait AttachmentService {

  def postAttachment(path: String): Either[Throwable, BacklogAttachment]

}
