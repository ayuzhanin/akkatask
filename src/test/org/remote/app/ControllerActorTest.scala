package org.remote.app

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.typesafe.config.ConfigFactory
import org.remote.app.controller.ControllerActor
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by one on 30.12.2016.
  */
class ControllerActorTest() extends TestKit(ActorSystem("TestSystem", ConfigFactory.load("testing.conf")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
      TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {
    "send back messages unchanged" in {
      val host = "testhost"
      val actor = system.actorOf(ControllerActor.props(host), s"${host}controller")
      actor ! "hello world"
      expectMsg("hello world")
    }
  }
}
