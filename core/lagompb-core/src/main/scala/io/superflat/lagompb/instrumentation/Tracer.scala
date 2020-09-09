package io.superflat.lagompb.instrumentation

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import kamon.Kamon
import kamon.trace.{Span, SpanBuilder}

object Tracer {

  def trace[Request, Response](
    serviceCall: ServerServiceCall[Request, Response]
  ): ServerServiceCall[Request, Response] = {
    ServerServiceCall.compose { requestHeader: RequestHeader =>
      {
        val uri = requestHeader.uri
        val method = requestHeader.method
        val spanBuilder: SpanBuilder = Kamon.spanBuilder(uri.getPath)
        spanBuilder.tag("http.method", method.toString())
        spanBuilder.tag("http.url", requestHeader.uri.toString)
        val span: Span = spanBuilder.start()
        Kamon.runWithSpan(span.takeSamplingDecision()) {
          serviceCall
        }
      }
    }
  }
}
