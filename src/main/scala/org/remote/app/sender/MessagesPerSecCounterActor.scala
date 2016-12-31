package org.remote.app.sender

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit._

import akka.actor.{Actor, ActorRef}
import org.remote.app.messaging.Statistics

/**
  * Created by one on 31.12.2016.
  */
class MessagesPerSecCounterActor extends Actor {
  def updateStatistics(stat: Statistics, map: Map[ActorRef, Statistics]) = {

    val now = LocalDateTime.now
    val updated = (map + (sender() -> stat)).filter { case (_, value) => Math.abs(MILLIS.between(value.date, now)) < 1000 }
    val messagesPerSecond = updated.values.map(_.number).sum
    println(messagesPerSecond)
    context become counting(updated)
  }

  override def receive: Receive = counting(Map[ActorRef, Statistics]())

  def counting(map: Map[ActorRef, Statistics]): Receive = {
    case stat: Statistics =>
      updateStatistics(stat, map)
  }
}
