package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogVersion

/**
  * @author uchida
  */
trait VersionService {

  def allVersions(): Seq[BacklogVersion]

  def add(version: BacklogVersion): Option[BacklogVersion]

  def update(versionId: Long, name: String): Option[BacklogVersion]

  def remove(versionId: Long)

}
