package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.{BacklogIssue, BacklogWiki}

/**
  * @author uchida
  */
trait SharedFileService {

  def linkIssueSharedFile(issueId: Long, backlogIssue: BacklogIssue)

  def linkWikiSharedFile(wikiId: Long, backlogWiki: BacklogWiki)

}
