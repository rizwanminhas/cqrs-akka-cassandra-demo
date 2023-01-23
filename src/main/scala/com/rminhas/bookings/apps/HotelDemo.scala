package com.rminhas.bookings.apps

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.rminhas.bookings.actor.Hotel
import com.rminhas.bookings.model._

import java.sql.Date
import java.util.UUID
import scala.concurrent.duration._

object HotelDemo {

  def main(args: Array[String]): Unit = {
    val simpleLogger = Behaviors.receive[Any] { (ctx, message) =>
      ctx.log.info(s"[logger] $message")
      Behaviors.same
    }

    val root = Behaviors.setup[String] { ctx =>
      val logger = ctx.spawn(simpleLogger, "logger") // child actor
      val hotel = ctx.spawn(Hotel("testHotel"), "testHotel")

      hotel ! MakeReservation(UUID.randomUUID().toString, Date.valueOf("2022-07-14"), Date.valueOf("2022-07-21"), 101, logger)
      hotel ! ChangeReservation("9B0KK6ABQR", Date.valueOf("2022-07-14"), Date.valueOf("2022-07-28"), 101, logger)
      hotel ! CancelReservation("9B0KK6ABQR", logger)
      Behaviors.empty
    }

    val system = ActorSystem(root, "DemoHotel")
    import system.executionContext
    system.scheduler.scheduleOnce(5.seconds, () => system.terminate())
  }

}