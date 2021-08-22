package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogSharedFile
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.SharedFile

/**
 * @author
 *   uchida
 */
private[common] class SharedFileWrites @Inject() ()
    extends Writes[SharedFile, BacklogSharedFile]
    with Logging {

  override def writes(sharedFile: SharedFile): BacklogSharedFile = {
    BacklogSharedFile(dir = sharedFile.getDir, name = sharedFile.getName)
  }

}
