package com.nulabinc.backlog.migration.common.service

import java.lang.Thread.sleep
import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.{CommentWrites, IssueWrites}
import com.nulabinc.backlog.migration.common.domain.IssueTags.SourceIssue
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.utils.{Logging, StringUtil}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.Issue.{PriorityType, ResolutionType}
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.{QueryParams, UpdateIssueParams}
import monix.eval.Task
import monix.execution.Scheduler

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class CommentServiceImpl @Inject() (
    implicit val issueWrites: IssueWrites,
    implicit val commentWrites: CommentWrites,
    implicit val s: Scheduler,
    implicit val consoleDSL: ConsoleDSL[Task],
    backlog: BacklogAPIClient,
    issueService: IssueService
) extends CommentService
    with Logging {

  override def allCommentsOfIssue(issueId: Long): Seq[BacklogComment] = {
    val allCount = backlog.getIssueCommentCount(issueId)

    def loop(
        optMinId: Option[Long],
        comments: Seq[IssueComment],
        offset: Long
    ): Seq[IssueComment] =
      if (offset < allCount) {
        sleep(200)
        val queryParams = new QueryParams()
        for { minId <- optMinId } yield {
          queryParams.minId(minId)
        }
        queryParams.count(100)
        queryParams.order(QueryParams.Order.Asc)
        val commentsPart =
          backlog.getIssueComments(issueId, queryParams).asScala
        val optLastId = for { lastComment <- commentsPart.lastOption } yield {
          lastComment.getId
        }
        loop(optLastId, comments concat commentsPart, offset + 100)
      } else comments

    loop(None, Seq.empty[IssueComment], 0)
      .sortWith((c1, c2) => c1.getCreated.before(c2.getCreated))
      .map(Convert.toBacklog(_))
  }

  override def update(
      setUpdateParam: BacklogComment => ImportUpdateIssueParams
  )(backlogComment: BacklogComment): Either[Throwable, BacklogComment] = {
    try {
      val noUpdate = updateIssue(setUpdateParam(backlogComment))
      if (noUpdate)
        logger.debug(
          s"    [Success Finish No Update Comment]:issueId[${backlogComment.optIssueId
            .getOrElse("")}] created[${backlogComment.optCreated.getOrElse("")}]----------------------------"
        )
      else
        logger.debug(
          s"    [Success Finish Create Comment]:issueId[${backlogComment.optIssueId
            .getOrElse("")}] created[${backlogComment.optCreated.getOrElse("")}]----------------------------"
        )
      Right(backlogComment)
    } catch {
      case e: Throwable =>
        logger.debug(
          s"    [Fail Finish Create Comment]:issueId[${backlogComment.optIssueId
            .getOrElse("")}] created[${backlogComment.optCreated.getOrElse("")}]----------------------------"
        )
        Left(e)
    }
  }

  override def setUpdateParam(
      issueId: Long,
      propertyResolver: PropertyResolver,
      toRemoteIssueId: (Long) => Option[Long],
      postAttachment: (String) => Option[Long]
  )(backlogComment: BacklogComment): ImportUpdateIssueParams = {
    logger.debug(
      s"    [Start Create Comment][Comment Date]:issueId[${issueId}] created[${backlogComment.optCreated
        .getOrElse("")}]"
    )

    val optCurrentIssue = issueService.optIssueOfId(issueId)
    val params          = new ImportUpdateIssueParams(issueId)

    //comment
    for { content <- backlogComment.optContent } yield {
      params.comment(content)
    }

    //notificationUserIds
    val notifiedUserIds = backlogComment.notifications
      .flatMap(_.optUser)
      .flatMap(_.optUserId)
      .flatMap(propertyResolver.optResolvedUserId)
    params.notifiedUserIds(notifiedUserIds.asJava)

    //created updated
    for { created <- backlogComment.optCreated } yield {
      params.created(created)
      params.updated(created)
    }

    //created updated user id
    for {
      createdUser <- backlogComment.optCreatedUser
      userId      <- createdUser.optUserId
      id          <- propertyResolver.optResolvedUserId(userId)
    } yield params.updatedUserId(id)

    //changelog
    backlogComment.changeLogs.foreach { changeLog =>
      setChangeLog(
        changeLog,
        params,
        toRemoteIssueId,
        propertyResolver,
        postAttachment,
        optCurrentIssue,
        backlogComment.optIssueId.map(Id[SourceIssue]),
        backlogComment.optCreated
      )
    }

    params
  }

  private[this] def updateIssue(params: ImportUpdateIssueParams): Boolean = {
    val paramList = params.getParamList.asScala
    paramList.foreach(p => logger.debug(s"        [Comment Parameter]:${p.getName}:${p.getValue}"))
    if (
      paramList.exists(p => p.getName == "created") &&
      paramList.exists(p => p.getName == "updated") &&
      paramList.exists(p => p.getName == "updatedUserId") &&
      paramList.size == 3
    ) {
      logger.warn("No update item")
      true
    } else {
      Convert.toBacklog(backlog.importUpdateIssue(params))
      false
    }
  }

  private def setChangeLog(
      changeLog: BacklogChangeLog,
      params: ImportUpdateIssueParams,
      toRemoteIssueId: (Long) => Option[Long],
      propertyResolver: PropertyResolver,
      postAttachment: (String) => Option[Long],
      optCurrentIssue: Option[Issue],
      optSrcIssueId: Option[Id[SourceIssue]],
      optCreated: Option[String]
  ) = {
    if (changeLog.optAttributeInfo.nonEmpty) {
      setCustomField(
        params,
        changeLog,
        propertyResolver,
        optSrcIssueId,
        optCreated
      )
    } else if (changeLog.optAttachmentInfo.nonEmpty) {
      setAttachment(params, changeLog, postAttachment)
    } else
      setAttr(
        params,
        changeLog,
        toRemoteIssueId,
        propertyResolver,
        optCurrentIssue
      )
  }

  private[this] def setAttr(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      toRemoteIssueId: Long => Option[Long],
      propertyResolver: PropertyResolver,
      optCurrentIssue: Option[Issue]
  ) =
    changeLog.field match {
      case BacklogConstantValue.ChangeLog.SUMMARY =>
        setSummary(params, changeLog)
      case BacklogConstantValue.ChangeLog.DESCRIPTION =>
        setDescription(params, changeLog)
      case BacklogConstantValue.ChangeLog.COMPONENT =>
        setCategory(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.VERSION =>
        setVersion(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.MILESTONE =>
        setMilestone(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.STATUS =>
        setStatus(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.ASSIGNER =>
        setAssignee(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.ISSUE_TYPE =>
        setIssueType(params, changeLog, propertyResolver, optCurrentIssue)
      case BacklogConstantValue.ChangeLog.START_DATE =>
        setStartDate(params, changeLog)
      case BacklogConstantValue.ChangeLog.LIMIT_DATE =>
        setDueDate(params, changeLog)
      case BacklogConstantValue.ChangeLog.PRIORITY =>
        setPriority(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.RESOLUTION =>
        setResolution(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.ESTIMATED_HOURS =>
        setEstimatedHours(params, changeLog)
      case BacklogConstantValue.ChangeLog.ACTUAL_HOURS =>
        setActualHours(params, changeLog)
      case BacklogConstantValue.ChangeLog.PARENT_ISSUE =>
        setParentIssue(params, changeLog, toRemoteIssueId)
      case BacklogConstantValue.ChangeLog.NOTIFICATION =>
      case BacklogConstantValue.ChangeLog.ATTACHMENT   =>
      case _ =>
        logger.warn(s"Unknown change log field type: ${changeLog.field}")
    }

  private[this] def setSummary(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog
  ) = {
    changeLog.optNewValue.map(value => params.summary(value))
  }

  private[this] def setDescription(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog
  ) =
    changeLog.optNewValue match {
      case Some(value) => params.description(value)
      case _           => params.description(null)
    }

  private[this] def setStartDate(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog
  ) =
    changeLog.optNewValue match {
      case Some(value) => params.startDate(value)
      case _           => params.startDate(null)
    }

  private[this] def setDueDate(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog
  ) =
    changeLog.optNewValue match {
      case Some(value) => params.dueDate(value)
      case _           => params.dueDate(null)
    }

  private[this] def setCategory(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ) =
    changeLog.optNewValue match {
      case Some("") => params.categoryIds(null)
      case Some(value) =>
        val ids =
          value.split(",").toSeq.map(_.trim).flatMap(propertyResolver.optResolvedCategoryId)
        if (ids.nonEmpty) params.categoryIds(ids.asJava)
        else params.categoryIds(null)
      case None => params.categoryIds(null)
    }

  private[this] def setVersion(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ) =
    changeLog.optNewValue match {
      case Some("") => params.versionIds(null)
      case Some(value) =>
        val ids =
          value.split(",").toSeq.flatMap(propertyResolver.optResolvedVersionId)
        if (ids.nonEmpty) params.versionIds(ids.asJava)
        else params.versionIds(null)
      case None => params.versionIds(null)
    }

  private[this] def setMilestone(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ) =
    changeLog.optNewValue match {
      case Some("") => params.milestoneIds(null)
      case Some(value) =>
        val ids =
          value.split(",").toSeq.flatMap(propertyResolver.optResolvedVersionId)
        if (ids.nonEmpty) params.milestoneIds(ids.asJava)
        else params.milestoneIds(null)
      case None => params.milestoneIds(null)
    }

  private[this] def setStatus(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ): Unit =
    changeLog.optNewValue.foreach { newValue =>
      params.statusId(
        propertyResolver.tryResolvedStatusId(BacklogStatusName(newValue)).value.toInt
      )
    }

  private[this] def setAssignee(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ): Object =
    changeLog.optNewValue match {
      case Some("") => params.assigneeId(-1L)
      case Some(value) =>
        for {
          id <- propertyResolver.optResolvedUserId(value)
        } yield params.assigneeId(id)
      case None => params.assigneeId(-1L)
    }

  private[this] def setIssueType(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver,
      optCurrentIssue: Option[Issue]
  ) =
    for {
      value        <- changeLog.optNewValue
      currentIssue <- optCurrentIssue
    } yield {
      for { id <- propertyResolver.optResolvedIssueTypeId(value) } yield {
        if (id != currentIssue.getIssueType.getId) {
          params.issueTypeId(id)
        }
      }
    }

  private[this] def setPriority(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ) =
    for {
      value <- changeLog.optNewValue
      priorityType <-
        propertyResolver
          .optResolvedPriorityId(value)
          .map(value => PriorityType.valueOf(value.toInt))
    } yield params.priority(priorityType)

  private[this] def setResolution(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver
  ) =
    for { value <- changeLog.optNewValue } yield {
      val optResolutionType = propertyResolver
        .optResolvedResolutionId(value)
        .map(value => ResolutionType.valueOf(value.toInt))
      params.resolution(optResolutionType.getOrElse(ResolutionType.NotSet))
    }

  private[this] def setEstimatedHours(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog
  ) =
    changeLog.optNewValue match {
      case Some("")    => params.estimatedHours(null)
      case Some(value) => params.estimatedHours(value.toFloat)
      case None        => params.estimatedHours(null)
    }

  private[this] def setActualHours(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog
  ) =
    changeLog.optNewValue match {
      case Some("")    => params.actualHours(null)
      case Some(value) => params.actualHours(value.toFloat)
      case None        => params.actualHours(null)
    }

  private[this] def setParentIssue(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      toRemoteIssueId: (Long) => Option[Long]
  ) =
    changeLog.optNewValue match {
      case Some(value) =>
        for {
          id <- toRemoteIssueId(value.toLong)
        } yield {
          val parentIssue = issueService.issueOfId(id)
          if (parentIssue.optParentIssueId.isEmpty) {
            params.parentIssueId(id)
          }
        }
      case None =>
        params.parentIssueId(UpdateIssueParams.PARENT_ISSUE_NOT_SET)
    }

  private[this] def setAttachment(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      postAttachment: (String) => Option[Long]
  ) =
    for {
      fileName <- changeLog.optAttachmentInfo.map(_.name)
      id       <- postAttachment(fileName)
    } yield params.attachmentIds(Seq(Long.box(id)).asJava)

  private def setCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      propertyResolver: PropertyResolver,
      optSrcIssueId: Option[Id[SourceIssue]],
      optCreated: Option[String]
  ) =
    for {
      customFieldSetting <- propertyResolver.optResolvedCustomFieldSetting(changeLog.field)
    } yield {
      FieldType.valueOf(customFieldSetting.typeId) match {
        case FieldType.Text =>
          setTextCustomField(params, changeLog, customFieldSetting)
        case FieldType.TextArea =>
          setTextCustomFieldArea(params, changeLog, customFieldSetting)
        case FieldType.Numeric =>
          setNumericCustomField(params, changeLog, customFieldSetting)
        case FieldType.Date =>
          setDateCustomField(params, changeLog, customFieldSetting)
        case FieldType.SingleList =>
          setSingleListCustomField(params, changeLog, customFieldSetting)
        case FieldType.MultipleList =>
          setMultipleListCustomField(
            params,
            changeLog,
            customFieldSetting,
            optSrcIssueId,
            optCreated
          )
        case FieldType.CheckBox =>
          setCheckBoxCustomField(params, changeLog, customFieldSetting)
        case FieldType.Radio =>
          setRadioCustomField(params, changeLog, customFieldSetting)
        case _ =>
      }
    }

  private def setTextCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ): Option[UpdateIssueParams] =
    for {
      id <- customFieldSetting.optId
      optParam = changeLog.optNewValue.map(params.textCustomField(id, _))
    } yield optParam.getOrElse(params.textCustomField(id, ""))

  private def setTextCustomFieldArea(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ): Option[UpdateIssueParams] =
    for {
      id <- customFieldSetting.optId
      optParam = changeLog.optNewValue.map(params.textAreaCustomField(id, _))
    } yield optParam.getOrElse(params.textAreaCustomField(id, ""))

  private[this] def setDateCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ) =
    (changeLog.optNewValue, customFieldSetting.optId) match {
      case (Some(""), Some(id))    => params.dateCustomField(id, null)
      case (Some(value), Some(id)) => params.dateCustomField(id, value)
      case (None, Some(id))        => params.dateCustomField(id, null)
      case _                       => throw new RuntimeException
    }

  private[this] def setNumericCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ) =
    (changeLog.optNewValue, customFieldSetting.optId) match {
      case (Some(""), Some(id)) => params.numericCustomField(id, null)
      case (Some(value), Some(id)) =>
        params.numericCustomField(id, StringUtil.safeUnitStringToFloat(value))
      case (None, Some(id)) => params.numericCustomField(id, null)
      case _                => throw new RuntimeException
    }

  private[this] def setCheckBoxCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ) =
    (
      changeLog.optNewValue,
      customFieldSetting.property,
      customFieldSetting.optId
    ) match {
      case (
            Some(value),
            property: BacklogCustomFieldMultipleProperty,
            Some(id)
          ) =>
        val newValues: Seq[String] = value.split(",").toSeq.map(_.trim)

        def findItem(newValue: String): Option[BacklogItem] = {
          property.items.find(_.name == newValue)
        }

        def isItem(value: String): Boolean = {
          findItem(value).isDefined
        }
        val listItems   = newValues.filter(isItem)
        val stringItems = newValues.filterNot(isItem)

        val itemIds = listItems.flatMap(findItem).flatMap(_.optId)
        params.checkBoxCustomField(id, itemIds.map(Long.box).asJava)
        params.customFieldOtherValue(id, stringItems.mkString(","))
      case _ =>
    }

  private[this] def setRadioCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ) =
    (
      changeLog.optNewValue,
      customFieldSetting.property,
      customFieldSetting.optId
    ) match {
      case (Some(value), property: BacklogCustomFieldMultipleProperty, Some(id))
          if value.nonEmpty =>
        for {
          item   <- property.items.find(_.name == value)
          itemId <- item.optId
        } yield params.radioCustomField(id, itemId)
      case _ =>
    }

  private def setSingleListCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting
  ) =
    for { id <- customFieldSetting.optId } yield {
      (changeLog.optNewValue, customFieldSetting.property) match {
        case (Some(value), property: BacklogCustomFieldMultipleProperty) if value.nonEmpty =>
          property.items
            .find(_.name == value)
            .flatMap(_.optId)
            .map { itemId =>
              params.singleListCustomField(id, itemId)
            }
            .getOrElse {
              ConsoleDSL[Task]
                .errorln(
                  s"Cannot find a custom status item. Item name: $value"
                )
                .runSyncUnsafe()
            }
          ()
        case _ =>
          params.emptySingleListCustomField(id)
      }
    }

  private def setMultipleListCustomField(
      params: ImportUpdateIssueParams,
      changeLog: BacklogChangeLog,
      customFieldSetting: BacklogCustomFieldSetting,
      optSrcIssueId: Option[Id[SourceIssue]],
      optCreated: Option[String]
  ): Unit =
    (
      changeLog.optNewValue,
      customFieldSetting.property,
      customFieldSetting.optId
    ) match {
      case (
            Some(value),
            property: BacklogCustomFieldMultipleProperty,
            Some(id)
          ) =>
        val newValues   = value.split(",").toSeq.map(_.trim)
        val listItems   = newValues.filter(isItemExists(property))
        val stringItems = newValues.filterNot(isItemExists(property))
        val itemIds     = listItems.flatMap(findItem(property)).flatMap(_.optId)

        // BLGMIGRATION-868
        newValues
          .diff(listItems)
          .filter(_.nonEmpty)
          .foreach { missingValue =>
            val srcIssueIdStr = optSrcIssueId.map(_.value).getOrElse("")
            val createdStr    = optCreated.getOrElse("")
            ConsoleDSL[Task]
              .errorln(
                s"Cannot find custom field value. Maybe it was renamed. Name: $missingValue Source issue id: $srcIssueIdStr Created: $createdStr"
              )
              .runSyncUnsafe()
          }

        params.multipleListCustomField(id, itemIds.map(Long.box).asJava)
        params.customFieldOtherValue(id, stringItems.mkString(","))
      case (None, _: BacklogCustomFieldMultipleProperty, Some(id)) =>
        params.multipleListCustomField(
          id,
          List.empty[Long].map(Long.box).asJava
        )
        params.customFieldOtherValue(id, "")
      case _ =>
        logger.warn("Unknown pattern of multiple list")
    }

  private[this] def findItem(
      property: BacklogCustomFieldMultipleProperty
  )(newValue: String): Option[BacklogItem] =
    property.items.find(_.name == newValue)

  private[this] def isItemExists(
      property: BacklogCustomFieldMultipleProperty
  )(target: String): Boolean =
    findItem(property)(target).isDefined

}
