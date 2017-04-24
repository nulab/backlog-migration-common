package com.nulabinc.backlog.migration.service

import java.io.InputStream
import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.converter.Backlog4jConverters
import com.nulabinc.backlog.migration.domain.{BacklogAttachment, BacklogWiki}
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.{AddWikiAttachmentParams, GetWikisParams, ImportWikiParams, UpdateWikiParams}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class WikiServiceImpl @Inject()(@Named("projectKey") projectKey: String, backlog: BacklogClient) extends WikiService with Logging {

  override def allWikis(): Seq[BacklogWiki] =
    backlog.getWikis(projectKey).asScala.map(Backlog4jConverters.Wiki.apply)

  override def wikiOfId(wikiId: Long): BacklogWiki =
    Backlog4jConverters.Wiki(backlog.getWiki(wikiId))

  override def update(wiki: BacklogWiki): Option[BacklogWiki] =
    for {
      wikiHome <- optWikiHome()
      content  <- wiki.optContent
    } yield {
      val params = new UpdateWikiParams(wikiHome.getId)
      params.name(wiki.name)
      params.content(content)
      params.mailNotify(false)
      Backlog4jConverters.Wiki(backlog.updateWiki(params))
    }

  override def create(projectId: Long, wiki: BacklogWiki, propertyResolver: PropertyResolver): BacklogWiki = {
    val params = new ImportWikiParams(projectId, wiki.name, wiki.optContent.getOrElse(""))

    //created
    for { created <- wiki.optCreated } yield params.created(created)

    //created user id
    for {
      createdUser <- wiki.optCreatedUser
      userId      <- createdUser.optUserId
      id          <- propertyResolver.optResolvedUserId(userId)
    } yield params.createdUserId(id)

    //updated
    for { updated <- wiki.optUpdated } yield params.updated(updated)

    //updated user id
    for {
      updatedUser <- wiki.optUpdatedUser
      userId      <- updatedUser.optUserId
      id          <- propertyResolver.optResolvedUserId(userId)
    } yield params.updatedUserId(id)

    Backlog4jConverters.Wiki(backlog.importWiki(params))
  }

  override def downloadWikiAttachment(wikiId: Long, attachmentId: Long): (String, InputStream) = {
    val attachmentData = backlog.downloadWikiAttachment(wikiId, attachmentId)
    (attachmentData.getFilename, attachmentData.getContent)
  }

  override def addAttachment(wikiId: Long, attachments: Seq[BacklogAttachment]): Either[Throwable, Seq[BacklogAttachment]] = {
    try {
      if (attachments.nonEmpty) {
        val params = new AddWikiAttachmentParams(wikiId, attachments.flatMap(_.optId).asJava)
        backlog.addWikiAttachment(params).asScala
      }
      Right(attachments)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

  private[this] def optWikiHome(): Option[Wiki] = {
    val params: GetWikisParams = new GetWikisParams(projectKey)
    backlog.getWikis(params).asScala.find(_.getName == BacklogConstantValue.WIKI_HOME_NAME)
  }

}
