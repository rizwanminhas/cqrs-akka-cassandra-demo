import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors

import scala.concurrent.duration.DurationInt

object Playground extends App {
  val system = ActorSystem(Behaviors.setup[String] { ctx =>
    ctx.getLog.info("Welcome to AKKA!")
    Behaviors.empty
  }, "SimpleSystem")

  import system.executionContext
  system.scheduler.scheduleOnce(3.seconds, () => system.terminate())
}
