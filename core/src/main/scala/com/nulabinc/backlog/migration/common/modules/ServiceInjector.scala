package com.nulabinc.backlog.migration.common.modules

import com.google.inject.{AbstractModule, Guice, Injector}
import com.nulabinc.backlog.migration.common.client.{BacklogAPIClient, BacklogAPIClientImpl}
import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.common.domain.BacklogProjectKey
import com.nulabinc.backlog.migration.common.service._
import com.nulabinc.backlog4j.conf.BacklogPackageConfigure

/**
  * @author uchida
  */
object ServiceInjector {

  def createInjector(apiConfig: BacklogApiConfiguration): Injector = {
    Guice.createInjector(new AbstractModule() {
      override def configure(): Unit = {
        val backlogPackageConfigure = new BacklogPackageConfigure(apiConfig.url)
        val configure               = backlogPackageConfigure.apiKey(apiConfig.key)
        val backlog                 = new BacklogAPIClientImpl(configure)

        bind(classOf[BacklogProjectKey]).toInstance(BacklogProjectKey(apiConfig.projectKey))
        bind(classOf[BacklogAPIClient]).toInstance(backlog)
        bind(classOf[ProjectService]).to(classOf[ProjectServiceImpl])
        bind(classOf[SpaceService]).to(classOf[SpaceServiceImpl])
        bind(classOf[UserService]).to(classOf[UserServiceImpl])
        bind(classOf[StatusService]).to(classOf[StatusServiceImpl])
        bind(classOf[PriorityService]).to(classOf[PriorityServiceImpl])
        bind(classOf[BacklogPaths]).toInstance(new BacklogPaths(apiConfig.projectKey))
      }
    })
  }

}
