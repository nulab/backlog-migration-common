package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.domain.{BacklogAttachment, BacklogWiki}

/**
 * @author
 *   uchida
 */
trait WikiService {

  def allWikis(): Seq[BacklogWiki]

  def wikiOfId(wikiId: Long): BacklogWiki

  def create(
      projectId: Long,
      wiki: BacklogWiki,
      propertyResolver: PropertyResolver
  ): BacklogWiki

  def update(wiki: BacklogWiki): Option[BacklogWiki]

  def downloadWikiAttachment(
      wikiId: Long,
      attachmentId: Long
  ): (String, InputStream)

  def addAttachment(
      wikiId: Long,
      attachments: Seq[BacklogAttachment]
  ): Either[Throwable, Seq[BacklogAttachment]]

}
