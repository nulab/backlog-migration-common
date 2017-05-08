package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.{BacklogIssue, BacklogWiki}

/**
  * @author uchida
  */
trait SharedFileService {

  def linkIssueSharedFile(issueId: Long, backlogIssue: BacklogIssue)

  def linkWikiSharedFile(wikiId: Long, backlogWiki: BacklogWiki)

}
