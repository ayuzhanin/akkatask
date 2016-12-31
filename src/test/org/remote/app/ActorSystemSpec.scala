package org.remote.app

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by one on 30.12.2016.
  */
class ActorSystemSpec(system: ActorSystem) extends TestKit(system)
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
//    TestKit.shutdownActorSystem()
  }
  def this() = this(ActorSystem("TestSystem", ConfigFactory.load("testing.conf")))
}
