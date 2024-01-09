package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.clientManager
import com.simiacryptus.skyenet.core.platform.ClientManager.RecordingThreadFactory
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionThreadsServlet(
    private val server: ApplicationServer,
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        if (req.parameterMap.containsKey("sessionId")) {
            val session = Session(req.getParameter("sessionId"))
            val user = authenticationManager.getUser(req.getCookie())
            val pool = clientManager.getPool(session, user, server.dataStorage)
            // Output all pool stack traces
            //language=HTML
            resp.writer.write(
                """
                    |<html>
                    |<head>
                    |    <title>Session Threads</title>
                    |    <style>
                    |        body {
                    |            font-family: 'Arial', sans-serif;
                    |            background-color: #f4f4f4;
                    |            color: #333;
                    |            margin: 0;
                    |            padding: 20px;
                    |        }
                    |
                    |        h1 {
                    |            color: #5d5d5d;
                    |        }
                    |
                    |        .pool-stats, .pool-threads {
                    |            background-color: #fff;
                    |            border: 1px solid #ddd;
                    |            padding: 15px;
                    |            margin-bottom: 20px;
                    |            border-radius: 4px;
                    |            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    |        }
                    |
                    |        .thread {
                    |            margin-bottom: 10px;
                    |            padding: 10px;
                    |            background-color: #e9e9e9;
                    |            border-radius: 4px;
                    |        }
                    |
                    |        .thread-name {
                    |            font-weight: bold;
                    |            color: #3a3a3a;
                    |        }
                    |
                    |        .stack-element {
                    |            background-color: #d9d9d9;
                    |            padding: 5px;
                    |            margin: 2px 0;
                    |            border-radius: 2px;
                    |            font-family: 'Courier New', monospace;
                    |            font-size: 0.9em;
                    |        }
                    |
                    |        p {
                    |            line-height: 1.6;
                    |        }
                    |
                    |        a {
                    |            color: #1a0dab;
                    |            text-decoration: none;
                    |        }
                    |
                    |        a:hover {
                    |            text-decoration: underline;
                    |        }
                    |
                    |        .pool-stats p, .pool-threads p {
                    |            margin: 5px 0;
                    |        }
                    |
                    |        .pool-stats p:first-child, .pool-threads p:first-child {
                    |            margin-top: 0;
                    |        }
                    |
                    |        .pool-stats p:last-child, .pool-threads p:last-child {
                    |            margin-bottom: 0;
                    |        }
                    |    </style>
                    |
                    |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
                    |</head>
                    |<body>
                    |<div class='pool-stats'>
                    |<h1>Pool Stats</h1>
                    |<p>Session: $session</p>
                    |<p>User: $user</p>
                    |<p>Pool: $pool</p>
                    |<p>Threads: ${pool.activeCount}/${pool.poolSize}</p>
                    |<p>Queue: ${pool.queue.size}/${pool.queue.remainingCapacity()}</p>
                    |<p>Completed: ${pool.completedTaskCount}</p>
                    |<p>Task Count: ${pool.taskCount}</p>
                    |</div>
                    |<div class='pool-threads'>
                    |<h1>Thread Stacks</h1>
                    |${
                    (pool.threadFactory as RecordingThreadFactory).threads.filter { it.isAlive }.joinToString("""<br/>""") { 
                        thread ->
                        """
                        <div class='thread'>
                        <div class='thread-name'>${thread.name}</div>
                        <div class='stack-trace'>${
                          thread.stackTrace.joinToString(separator = "\n")
                          { stackTraceElement -> """<div class='stack-element'>$stackTraceElement</div>""" }
                        }</div>
                        </div>
                        """
                    }
                    }
                    |</div>
                    |</body>
                    |</html>
                    """.trimMargin()
            )
          // (pool.threadFactory as RecordingThreadFactory).threads
        } else {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        }
    }
}