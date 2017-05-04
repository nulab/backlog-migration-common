package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogSharedFile
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.SharedFile

/**
  * @author uchida
  */
class SharedFileWrites @Inject()() extends Writes[SharedFile, BacklogSharedFile] with Logging {

  override def writes(sharedFile: SharedFile): BacklogSharedFile = {
    BacklogSharedFile(dir = sharedFile.getDir, name = sharedFile.getName)
  }

}