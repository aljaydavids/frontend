package conf

import com.gu.conf.ConfigurationFactory
import com.gu.openplatform.contentapi.connection.MultiThreadedApacheHttpClient
import com.gu.openplatform.contentapi.model.Content
import com.gu.openplatform.contentapi.{ ApiError, Api }
import content.Article
import java.util.logging.Logger

object Configuration {

  private val configuration = ConfigurationFactory getConfiguration "article-server"

  object contentApi {
    lazy val host = configuration.getStringProperty("content.api.host") getOrElse {
      throw new IllegalStateException("Content Api Host not configured")
    }

    lazy val key = configuration.getStringProperty("content.api.key") getOrElse {
      throw new IllegalStateException("Content Api Key not configured")
    }
  }
}

object ContentApi extends Api with MultiThreadedApacheHttpClient {
  import Configuration._

  override val targetUrl = contentApi.host
  apiKey = Some(contentApi.key)

  maxConnections(1000)

  override protected def fetch(url: String, parameters: Map[String, Any]) = {
    super.fetch(url, parameters + ("user-tier" -> "internal"))
  }
}

//TODO we need to put a proper logging framework in
trait Logging {

  private val _log = Logger.getLogger(getClass.getName)

  object log {
    def info(msg: String) = _log.info(msg)
  }
}

object `package` extends Logging {
  implicit def content2IsArticle(content: Content) = new {
    lazy val isArticle = content.tags.exists(_.id == "type/article")
  }

  def suppressApi404(block: => Option[Article]) = {
    try {
      block
    } catch {
      case ApiError(404, _) => {
        log.info("Got a 404 while calling content api")
        None
      }
    }
  }
}