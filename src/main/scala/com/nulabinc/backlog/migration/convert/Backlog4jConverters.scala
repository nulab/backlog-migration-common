package com.nulabinc.backlog.migration.convert

import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.{DateUtil, FileUtil, Logging}
import com.nulabinc.backlog4j._

/**
  * @author uchida
  */
object Backlog4jConverters extends Logging {

  object Attachment {
    def apply(attachment: Attachment): BacklogAttachment =
      BacklogAttachment(
        optId = Some(attachment.getId),
        name = FileUtil.normalize(attachment.getName)
      )
  }

  object Space {
    def apply(space: Space): BacklogSpace = {
      BacklogSpace(
        spaceKey = space.getSpaceKey,
        name = space.getName,
        created = DateUtil.isoFormat(space.getCreated)
      )
    }
  }

  object Environment {
    def apply(environment: Environment): BacklogEnvironment = {
      BacklogEnvironment(
        name = environment.getName,
        spaceId = environment.getSpaceId
      )
    }
  }

}
