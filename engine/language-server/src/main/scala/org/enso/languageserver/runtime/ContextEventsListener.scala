package org.enso.languageserver.runtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.enso.languageserver.data.Config
import org.enso.languageserver.runtime.ExecutionApi.ContextId
import org.enso.languageserver.runtime.VisualisationProtocol.{
  VisualisationContext,
  VisualisationUpdate
}
import org.enso.languageserver.session.RpcSession
import org.enso.languageserver.session.SessionRouter.DeliverToDataController
import org.enso.languageserver.util.UnhandledLogging
import org.enso.polyglot.runtime.Runtime.Api

/**
  * EventListener listens event stream for the notifications from the runtime
  * and send updates to the client. The listener is created per context, and
  * only handles the notifications with the given `contextId`.
  *
  * @param config configuration
  * @param rpcSession reference to the client
  * @param contextId exectuion context identifier
  * @param sessionRouter the session router
  */
final class ContextEventsListener(
  config: Config,
  rpcSession: RpcSession,
  contextId: ContextId,
  sessionRouter: ActorRef
) extends Actor
    with ActorLogging
    with UnhandledLogging {

  override def preStart(): Unit = {
    context.system.eventStream
      .subscribe(self, classOf[Api.ExpressionValuesComputed])
    context.system.eventStream
      .subscribe(self, classOf[Api.VisualisationUpdate])
  }

  override def receive: Receive = {
    case Api.VisualisationUpdate(ctx, data) if ctx.contextId == contextId =>
      val payload =
        VisualisationUpdate(
          VisualisationContext(
            ctx.visualisationId,
            ctx.contextId,
            ctx.expressionId
          ),
          data
        )
      sessionRouter ! DeliverToDataController(rpcSession.clientId, payload)

    case Api.ExpressionValuesComputed(`contextId`, apiUpdates) =>
      val updates = apiUpdates.flatMap { update =>
        toRuntimeUpdate(update) match {
          case None =>
            log.error(s"Failed to convert $update")
            None
          case runtimeUpdate =>
            runtimeUpdate
        }
      }
      rpcSession.rpcController ! ContextRegistryProtocol
        .ExpressionValuesComputedNotification(
          contextId,
          updates
        )

    case _: Api.ExpressionValuesComputed =>
    case _: Api.VisualisationUpdate      =>
  }

  private def toRuntimeUpdate(
    update: Api.ExpressionValueUpdate
  ): Option[ExpressionValueUpdate] = {
    update.methodCall match {
      case None =>
        Some(
          ExpressionValueUpdate(
            update.expressionId,
            update.expressionType,
            update.shortValue,
            None
          )
        )
      case Some(methodCall) =>
        toRuntimePointer(methodCall).map { pointer =>
          ExpressionValueUpdate(
            update.expressionId,
            update.expressionType,
            update.shortValue,
            Some(pointer)
          )
        }
    }
  }

  private def toRuntimePointer(
    pointer: Api.MethodPointer
  ): Option[MethodPointer] =
    config.findRelativePath(pointer.file).map { relativePath =>
      MethodPointer(
        file          = relativePath,
        definedOnType = pointer.definedOnType,
        name          = pointer.name
      )
    }

}

object ContextEventsListener {

  /**
    * Creates a configuration object used to create a [[ContextEventsListener]].
    *
    * @param config configuration
    * @param rpcSession reference to the client
    * @param contextId exectuion context identifier
    * @param sessionRouter the session router
    */
  def props(
    config: Config,
    rpcSession: RpcSession,
    contextId: ContextId,
    sessionRouter: ActorRef
  ): Props =
    Props(
      new ContextEventsListener(
        config,
        rpcSession,
        contextId,
        sessionRouter: ActorRef
      )
    )

}
