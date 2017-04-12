package com.nulabinc.backlog.migration.modules

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Injector, Provider}
import com.nulabinc.backlog.migration.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.modules.AkkaModule.ActorSystemProvider
import com.nulabinc.backlog.migration.modules.akkaguice.GuiceAkkaExtension
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule

object AkkaModule {

  class ActorSystemProvider @Inject()(val config: Config, val injector: Injector) extends Provider[ActorSystem] with BacklogConfiguration {
    override def get() = {
      val system = ActorSystem.apply("main-actor-system", getBacklogConfiguration().getConfig("backlog.migration"))
      // add the GuiceAkkaExtension to the system, and initialize it with the Guice injector
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }

}

/**
  * A module providing an Akka ActorSystem.
  */
class AkkaModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
  }
}
